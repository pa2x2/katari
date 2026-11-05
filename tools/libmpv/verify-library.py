"""Plays test media through a built libmpv.so the way the desktop app uses it, without opening a window or a sound.

    verify-library.py <libmpv.so> <media directory> <C compiler>

The test media is made into the directory once (it is never part of the repository), by FFmpeg's command line tool:
an H.264 clip with AAC audio and an ASS subtitle, and the same as HLS and as DASH, served over loopback HTTP as the
app's stream proxy serves streams. An MP4 subtitle file carrying WebVTT ('wvtt', as DASH subtitles come), which only
the fork's FFmpeg patch reads, is written here, as no FFmpeg muxer writes one. Each is played with mpv's null outputs,
or drawn through the render API, and must play to its end.

Checked on top: the subtitle draws through libass into a software-rendered frame (the render API's SW path); the
demuxer cache fills; mpv lists this machine's PipeWire, PulseAudio and ALSA devices, which only works when it opened
those libraries; VA-API (copy-back) and Vulkan decoding work where this machine has them; and VA-API frames reach
OpenGL without a copy, drawn by the render API into an off-screen EGL context with the GPU's DRM render node as its
display, as the desktop player will draw. Then all of it again, but the hardware, in a process where the system
libraries libmpv opens itself cannot be found, as on a system without them: everything but the missing features must
still work, except that libass, without fontconfig, has no fonts to draw a subtitle with.
"""

import ctypes
import functools
import http.server
import json
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import threading
import time
from pathlib import Path

HIDDEN_FLAG = "--without-system-libraries"
hidden = sys.argv[1:2] == [HIDDEN_FLAG]
arguments = sys.argv[2:] if hidden else sys.argv[1:]
if len(arguments) != 3:
    sys.exit(__doc__)
library_path, media, compiler = Path(arguments[0]).resolve(), Path(arguments[1]), arguments[2]
here = Path(__file__).resolve().parent

# The libraries libmpv opens itself, and the Vulkan loader FFmpeg opens (by either name).
SYSTEM_LIBRARIES = [
    line.strip() for line in (here / "system-libraries.txt").read_text().splitlines()
    if line.split("#", 1)[0].strip() and not line[:1].isspace()
] + ["libvulkan.so.1", "libvulkan.so"]

WIDTH, HEIGHT = 640, 360
# A white box over the top half of the picture, drawn by libass without needing a font, then a line of text.
ASS = """[Script Info]
ScriptType: v4.00+
PlayResX: 640
PlayResY: 360

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, \
ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,sans-serif,32,&H00FFFFFF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,0,0,2,10,10,10,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
Dialogue: 0,0:00:00.00,0:00:03.00,Default,,0,0,0,,{\\an7\\pos(0,0)\\bord0\\shad0\\p1}m 0 0 l 640 0 640 180 0 180{\\p0}
Dialogue: 0,0:00:00.00,0:00:03.00,Default,,0,0,0,,Katari
"""


def make_media():
    """Makes the test media unless the directory has it from an earlier run."""
    done = media / "done"
    if done.exists():
        return
    if not shutil.which("ffmpeg"):
        sys.exit("verify-library.py needs FFmpeg's command line tool to make its test media")
    shutil.rmtree(media, ignore_errors=True)
    (media / "hls").mkdir(parents=True)
    (media / "dash").mkdir()
    (media / "clip.ass").write_text(ASS)

    def ffmpeg(*arguments):
        subprocess.run(["ffmpeg", "-nostdin", "-loglevel", "error", "-y", *arguments], check=True, cwd=media)

    ffmpeg("-f", "lavfi", "-i", f"testsrc2=size={WIDTH}x{HEIGHT}:rate=25:duration=3",
           "-f", "lavfi", "-i", "sine=frequency=440:duration=3", "-i", "clip.ass",
           "-map", "0", "-map", "1", "-map", "2", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-g", "25",
           "-c:a", "aac", "-c:s", "ass", "clip.mkv")
    ffmpeg("-i", "clip.mkv", "-map", "0:v", "-map", "0:a", "-c", "copy", "-f", "hls", "-hls_time", "1",
           "-hls_playlist_type", "vod", "-hls_segment_filename", "hls/segment%d.ts", "hls/index.m3u8")
    ffmpeg("-i", "clip.mkv", "-map", "0:v", "-map", "0:a", "-c", "copy", "-f", "dash", "-seg_duration", "1",
           "dash/manifest.mpd")
    (media / "wvtt.mp4").write_bytes(webvtt_mp4("Katari", 2500, 3000))
    done.touch()


def box(kind: bytes, *parts: bytes) -> bytes:
    payload = b"".join(parts)
    return struct.pack(">I", 8 + len(payload)) + kind + payload


def full_box(kind: bytes, version_flags: int, *parts: bytes) -> bytes:
    return box(kind, struct.pack(">I", version_flags), *parts)


def webvtt_mp4(text: str, cue_ms: int, duration_ms: int) -> bytes:
    """An MP4 with one ISO/IEC 14496-30 WebVTT track, which FFmpeg's muxers cannot write: a cue, then a gap."""
    samples = [box(b"vttc", box(b"payl", text.encode())), box(b"vtte")]
    matrix = struct.pack(">9I", 0x10000, 0, 0, 0, 0x10000, 0, 0, 0, 0x40000000)

    def movie(chunk_offset: int) -> bytes:
        sample_table = box(
            b"stbl",
            full_box(b"stsd", 0, struct.pack(">I", 1),
                     box(b"wvtt", bytes(6), struct.pack(">H", 1), box(b"vttC", b"WEBVTT"))),
            full_box(b"stts", 0, struct.pack(">5I", 2, 1, cue_ms, 1, duration_ms - cue_ms)),
            full_box(b"stsc", 0, struct.pack(">4I", 1, 1, len(samples), 1)),
            full_box(b"stsz", 0, struct.pack(">2I", 0, len(samples)), *[struct.pack(">I", len(s)) for s in samples]),
            full_box(b"stco", 0, struct.pack(">2I", 1, chunk_offset)),
        )
        media_information = box(b"minf", full_box(b"nmhd", 0),
                                box(b"dinf", full_box(b"dref", 0, struct.pack(">I", 1), full_box(b"url ", 1))),
                                sample_table)
        track_media = box(b"mdia", full_box(b"mdhd", 0, struct.pack(">4I2H", 0, 0, 1000, duration_ms, 0x55C4, 0)),
                          full_box(b"hdlr", 0, bytes(4), b"text", bytes(12), b"WebVTT\0"), media_information)
        track = box(b"trak", full_box(b"tkhd", 3, struct.pack(">5I", 0, 0, 1, 0, duration_ms), bytes(8),
                                      struct.pack(">2h2H", 0, 0, 0, 0), matrix, struct.pack(">2I", 0, 0)),
                    track_media)
        header = full_box(b"mvhd", 0, struct.pack(">4IIH", 0, 0, 1000, duration_ms, 0x10000, 0x100), bytes(10),
                          matrix, bytes(24), struct.pack(">I", 2))
        return box(b"moov", header, track)

    file_type = box(b"ftyp", b"iso6", struct.pack(">I", 0), b"iso6mp41")
    chunk_offset = len(file_type) + len(movie(0)) + 8
    return file_type + movie(chunk_offset) + box(b"mdat", *samples)


# libmpv's C API, as much as the checks use.

class RenderParam(ctypes.Structure):
    _fields_ = [("type", ctypes.c_int), ("data", ctypes.c_void_p)]


class Event(ctypes.Structure):
    _fields_ = [("event_id", ctypes.c_int), ("error", ctypes.c_int), ("reply_userdata", ctypes.c_uint64),
                ("data", ctypes.c_void_p)]


class EndFile(ctypes.Structure):
    _fields_ = [("reason", ctypes.c_int), ("error", ctypes.c_int)]


class LogMessage(ctypes.Structure):
    _fields_ = [("prefix", ctypes.c_char_p), ("level", ctypes.c_char_p), ("text", ctypes.c_char_p)]


GetProcAddress = ctypes.CFUNCTYPE(ctypes.c_void_p, ctypes.c_void_p, ctypes.c_char_p)


class OpenGlInitParams(ctypes.Structure):
    _fields_ = [("get_proc_address", GetProcAddress), ("get_proc_address_ctx", ctypes.c_void_p)]


class OpenGlFbo(ctypes.Structure):
    _fields_ = [("fbo", ctypes.c_int), ("w", ctypes.c_int), ("h", ctypes.c_int), ("internal_format", ctypes.c_int)]


class DrmDisplayV2(ctypes.Structure):
    _fields_ = [("fd", ctypes.c_int), ("crtc_id", ctypes.c_int), ("connector_id", ctypes.c_int),
                ("atomic_request_ptr", ctypes.c_void_p), ("render_fd", ctypes.c_int)]


EVENT_SHUTDOWN, EVENT_LOG_MESSAGE, EVENT_END_FILE, EVENT_PLAYBACK_RESTART = 1, 2, 7, 21
END_FILE_REASON_EOF = 0
PARAM_API_TYPE, PARAM_OPENGL_INIT_PARAMS, PARAM_OPENGL_FBO, PARAM_FLIP_Y = 1, 2, 3, 4
PARAM_BLOCK_FOR_TARGET_TIME, PARAM_DRM_DISPLAY_V2 = 12, 16
PARAM_SW_SIZE, PARAM_SW_FORMAT, PARAM_SW_STRIDE, PARAM_SW_POINTER = 17, 18, 19, 20
RENDER_UPDATE_FRAME = 1

mpv = ctypes.CDLL(str(library_path))
mpv.mpv_create.restype = ctypes.c_void_p
for name, restype, argtypes in [
    ("mpv_initialize", ctypes.c_int, [ctypes.c_void_p]),
    ("mpv_set_option_string", ctypes.c_int, [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p]),
    ("mpv_set_property_string", ctypes.c_int, [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p]),
    ("mpv_get_property_string", ctypes.c_void_p, [ctypes.c_void_p, ctypes.c_char_p]),
    ("mpv_command", ctypes.c_int, [ctypes.c_void_p, ctypes.POINTER(ctypes.c_char_p)]),
    ("mpv_wait_event", ctypes.POINTER(Event), [ctypes.c_void_p, ctypes.c_double]),
    ("mpv_request_log_messages", ctypes.c_int, [ctypes.c_void_p, ctypes.c_char_p]),
    ("mpv_error_string", ctypes.c_char_p, [ctypes.c_int]),
    ("mpv_free", None, [ctypes.c_void_p]),
    ("mpv_terminate_destroy", None, [ctypes.c_void_p]),
    ("mpv_render_context_create", ctypes.c_int,
     [ctypes.POINTER(ctypes.c_void_p), ctypes.c_void_p, ctypes.POINTER(RenderParam)]),
    ("mpv_render_context_update", ctypes.c_uint64, [ctypes.c_void_p]),
    ("mpv_render_context_render", ctypes.c_int, [ctypes.c_void_p, ctypes.POINTER(RenderParam)]),
    ("mpv_render_context_free", None, [ctypes.c_void_p]),
]:
    function = getattr(mpv, name)
    function.restype, function.argtypes = restype, argtypes


def render_params(*pairs) -> ctypes.Array:
    params = (RenderParam * (len(pairs) + 1))()
    for index, (kind, value) in enumerate(pairs):
        params[index] = RenderParam(kind, ctypes.cast(value, ctypes.c_void_p))
    return params


class Player:
    """One mpv instance with the app's kind of options and its warnings collected."""

    def __init__(self, **options):
        self.handle = mpv.mpv_create()
        self.log: list[str] = []
        self.render_context = None
        defaults = {"config": "no", "terminal": "no", "ao": "null", "vo": "null", "idle": "yes", "keep-open": "no",
                    "input-default-bindings": "no", "sub-auto": "no",
                    "audio-file-auto": "no"}
        for name, value in {**defaults, **options}.items():
            self.check(mpv.mpv_set_option_string(self.handle, name.encode(), str(value).encode()), name)
        self.check(mpv.mpv_initialize(self.handle), "mpv_initialize")
        mpv.mpv_request_log_messages(self.handle, b"warn")

    @staticmethod
    def check(result: int, what: str):
        if result < 0:
            raise RuntimeError(f"{what}: {mpv.mpv_error_string(result).decode()}")

    def command(self, *words: str):
        array = (ctypes.c_char_p * (len(words) + 1))(*[word.encode() for word in words], None)
        self.check(mpv.mpv_command(self.handle, array), words[0])

    def get(self, name: str) -> str | None:
        pointer = mpv.mpv_get_property_string(self.handle, name.encode())
        if not pointer:
            return None
        try:
            return ctypes.string_at(pointer).decode()
        finally:
            mpv.mpv_free(pointer)

    def set(self, name: str, value: str):
        self.check(mpv.mpv_set_property_string(self.handle, name.encode(), value.encode()), name)

    def event(self, timeout: float) -> tuple[int, int, int]:
        """The next event's id, end-file reason and error, logging warnings."""
        event = mpv.mpv_wait_event(self.handle, timeout).contents
        reason = error = 0
        if event.event_id == EVENT_LOG_MESSAGE:
            message = ctypes.cast(event.data, ctypes.POINTER(LogMessage)).contents
            self.log.append(f"[{message.prefix.decode()}] {message.text.decode(errors='replace').rstrip()}")
        elif event.event_id == EVENT_END_FILE:
            end = ctypes.cast(event.data, ctypes.POINTER(EndFile)).contents
            reason, error = end.reason, end.error
        return event.event_id, reason, error

    def create_render_context(self, *pairs):
        context = ctypes.c_void_p()
        self.check(mpv.mpv_render_context_create(ctypes.byref(context), self.handle, render_params(*pairs)),
                   "mpv_render_context_create")
        self.render_context = context

    def close(self):
        if self.render_context:
            mpv.mpv_render_context_free(self.render_context)
        mpv.mpv_terminate_destroy(self.handle)


def failure(what: str, player: Player | None = None) -> RuntimeError:
    log = "".join(f"\n    {line}" for line in (player.log[-15:] if player else []))
    return RuntimeError(what + log)


def play(url: str, until_end: bool = True, **options) -> dict[str, str | None]:
    """Plays url with null outputs, as fast as it decodes, and returns properties read once playback started."""
    player = Player(untimed="yes", **{"ao-null-untimed": "yes"}, **options)
    try:
        player.command("loadfile", url)
        started: dict[str, str | None] = {}
        deadline = time.monotonic() + 30
        while time.monotonic() < deadline:
            event, reason, error = player.event(0.5)
            if event == EVENT_PLAYBACK_RESTART and not started:
                started = {name: player.get(name) for name in
                           ["track-list", "file-format", "hwdec-current", "demuxer-cache-state", "audio-device-list",
                            "demuxer-via-network"]}
                if not until_end:
                    return started
            elif event == EVENT_END_FILE:
                if reason != END_FILE_REASON_EOF or error < 0 or not started:
                    raise failure(f"{url} did not play to its end (reason {reason}, error {error})", player)
                return started
        raise failure(f"{url} did not finish in time", player)
    finally:
        player.close()


def tracks(started: dict) -> list[tuple[str, str]]:
    return [(track["type"], track.get("codec", "")) for track in json.loads(started["track-list"])]


def render_frames(player: Player, render, seconds: float):
    """Renders each frame mpv offers for a while, as a player redrawing on mpv's updates does."""
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        player.event(0.01)
        if mpv.mpv_render_context_update(player.render_context) & RENDER_UPDATE_FRAME:
            render()


def load_paused(player: Player, url: str):
    player.command("loadfile", url)
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        event, reason, error = player.event(0.05)
        if event == EVENT_PLAYBACK_RESTART:
            return
        if event == EVENT_END_FILE:
            raise failure(f"{url} ended before it played (reason {reason}, error {error})", player)
    raise failure(f"{url} did not start in time", player)


def check_subtitles_drawn(clip: str) -> str:
    """Draws a frame through the software render API with the subtitle shown and hidden."""
    player = Player(vo="libmpv", pause="yes", start="1", sid="1")
    try:
        size = (ctypes.c_int * 2)(WIDTH, HEIGHT)
        stride = ctypes.c_size_t(WIDTH * 4)
        pixels = ctypes.create_string_buffer(WIDTH * 4 * HEIGHT)
        player.create_render_context((PARAM_API_TYPE, ctypes.c_char_p(b"sw")))
        render_args = render_params((PARAM_SW_SIZE, size), (PARAM_SW_FORMAT, ctypes.c_char_p(b"rgb0")),
                                    (PARAM_SW_STRIDE, ctypes.pointer(stride)), (PARAM_SW_POINTER, pixels))

        def render():
            player.check(mpv.mpv_render_context_render(player.render_context, render_args), "render")

        def top_brightness() -> float:
            top = pixels.raw[:WIDTH * 4 * HEIGHT // 2]
            return sum(top[index] for index in range(0, len(top), 4 * 7)) / (len(top) / (4 * 7))

        load_paused(player, clip)
        render_frames(player, render, 1.0)
        render()
        shown = top_brightness()
        player.set("sub-visibility", "no")
        render_frames(player, render, 1.0)
        render()
        without = top_brightness()
        if hidden:
            # Without fontconfig libass has no fonts but the ones a subtitle file carries, and draws nothing else.
            if not 50 < without < 200 or shown != without:
                raise failure(f"top half brightness {shown:.0f} shown, {without:.0f} hidden", player)
            return "the picture draws; without fonts libass draws no subtitle, as expected"
        if not (shown > 250 and without < 200):
            raise failure(f"the subtitle box did not draw: top half brightness {shown:.0f} shown, "
                          f"{without:.0f} hidden", player)
        return f"top half brightness {shown:.0f} with the subtitle, {without:.0f} without"
    finally:
        player.close()


def check_vaapi_into_opengl(clip: str) -> str | None:
    """Draws VA-API frames into an FBO of an off-screen EGL context; None when this machine can't."""
    render_nodes = sorted(Path("/dev/dri").glob("renderD*"))
    try:
        egl = ctypes.CDLL("libEGL.so.1")
    except OSError:
        return None
    if not render_nodes:
        return None
    egl.eglGetProcAddress.restype = ctypes.c_void_p
    egl.eglGetProcAddress.argtypes = [ctypes.c_char_p]
    egl.eglQueryString.restype = ctypes.c_char_p
    egl.eglQueryString.argtypes = [ctypes.c_void_p, ctypes.c_int]
    if b"EGL_MESA_platform_surfaceless" not in (egl.eglQueryString(None, 0x3055) or b""):
        return None

    def function(name: str, restype, *argtypes):
        address = egl.eglGetProcAddress(name.encode())
        if not address:
            raise RuntimeError(f"no {name}")
        return ctypes.CFUNCTYPE(restype, *argtypes)(address)

    c_int, c_uint, c_void_p = ctypes.c_int, ctypes.c_uint, ctypes.c_void_p
    get_platform_display = function("eglGetPlatformDisplayEXT", c_void_p, c_uint, c_void_p, c_void_p)
    initialize = function("eglInitialize", c_uint, c_void_p, c_void_p, c_void_p)
    bind_api = function("eglBindAPI", c_uint, c_uint)
    choose_config = function("eglChooseConfig", c_uint, c_void_p, c_void_p, c_void_p, c_int, c_void_p)
    create_context = function("eglCreateContext", c_void_p, c_void_p, c_void_p, c_void_p, c_void_p)
    make_current = function("eglMakeCurrent", c_uint, c_void_p, c_void_p, c_void_p, c_void_p)
    destroy_context = function("eglDestroyContext", c_uint, c_void_p, c_void_p)
    terminate = function("eglTerminate", c_uint, c_void_p)

    display = get_platform_display(0x31DD, None, None)  # EGL_PLATFORM_SURFACELESS_MESA
    if not display or not initialize(display, None, None) or not bind_api(0x30A2):  # EGL_OPENGL_API
        return None
    config, count = c_void_p(), c_int()
    # EGL_SURFACE_TYPE 0 (any, instead of windows), EGL_RENDERABLE_TYPE EGL_OPENGL_BIT, EGL_NONE
    attributes = (c_int * 5)(0x3033, 0, 0x3040, 0x0008, 0x3038)
    if not choose_config(display, attributes, ctypes.byref(config), 1, ctypes.byref(count)) or count.value < 1:
        terminate(display)
        return None
    context = create_context(display, config, None, None)
    if not context or not make_current(display, None, None, context):
        terminate(display)
        return None

    gen_framebuffers = function("glGenFramebuffers", None, c_int, c_void_p)
    bind_framebuffer = function("glBindFramebuffer", None, c_uint, c_uint)
    gen_textures = function("glGenTextures", None, c_int, c_void_p)
    bind_texture = function("glBindTexture", None, c_uint, c_uint)
    tex_image = function("glTexImage2D", None, c_uint, c_int, c_int, c_int, c_int, c_int, c_uint, c_uint, c_void_p)
    framebuffer_texture = function("glFramebufferTexture2D", None, c_uint, c_uint, c_uint, c_uint, c_int)
    framebuffer_status = function("glCheckFramebufferStatus", c_uint, c_uint)
    read_pixels = function("glReadPixels", None, c_int, c_int, c_int, c_int, c_uint, c_uint, c_void_p)
    finish = function("glFinish", None)

    texture, framebuffer = c_uint(), c_uint()
    gen_textures(1, ctypes.byref(texture))
    bind_texture(0x0DE1, texture.value)  # GL_TEXTURE_2D
    tex_image(0x0DE1, 0, 0x8058, WIDTH, HEIGHT, 0, 0x1908, 0x1401, None)  # GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE
    gen_framebuffers(1, ctypes.byref(framebuffer))
    bind_framebuffer(0x8D40, framebuffer.value)  # GL_FRAMEBUFFER
    framebuffer_texture(0x8D40, 0x8CE0, 0x0DE1, texture.value, 0)  # GL_COLOR_ATTACHMENT0
    if framebuffer_status(0x8D40) != 0x8CD5:  # GL_FRAMEBUFFER_COMPLETE
        raise RuntimeError("the test framebuffer is incomplete")

    render_fd = os.open(render_nodes[0], os.O_RDWR | os.O_CLOEXEC)
    proc_address = GetProcAddress(lambda _, name: egl.eglGetProcAddress(name))
    player = Player(vo="libmpv", hwdec="vaapi", pause="yes", start="1")
    try:
        init = OpenGlInitParams(proc_address, None)
        drm = DrmDisplayV2(-1, 0, 0, None, render_fd)
        player.create_render_context((PARAM_API_TYPE, ctypes.c_char_p(b"opengl")),
                                     (PARAM_OPENGL_INIT_PARAMS, ctypes.pointer(init)),
                                     (PARAM_DRM_DISPLAY_V2, ctypes.pointer(drm)))
        fbo = OpenGlFbo(framebuffer.value, WIDTH, HEIGHT, 0)
        flip, block = c_int(0), c_int(0)
        render_args = render_params((PARAM_OPENGL_FBO, ctypes.pointer(fbo)), (PARAM_FLIP_Y, ctypes.pointer(flip)),
                                    (PARAM_BLOCK_FOR_TARGET_TIME, ctypes.pointer(block)))
        pixels = ctypes.create_string_buffer(WIDTH * HEIGHT * 4)

        def render():
            player.check(mpv.mpv_render_context_render(player.render_context, render_args), "render")
            finish()

        load_paused(player, clip)
        render_frames(player, render, 1.0)
        render()
        bind_framebuffer(0x8D40, framebuffer.value)
        read_pixels(0, 0, WIDTH, HEIGHT, 0x1908, 0x1401, pixels)
        current = player.get("hwdec-current")
        distinct = len(set(pixels.raw[index:index + 3] for index in range(0, len(pixels.raw), 4 * 97)))
        if current != "vaapi" or distinct < 16:
            raise failure(f"VA-API into OpenGL: hwdec-current {current}, {distinct} distinct colours drawn", player)
        return f"hwdec-current vaapi, {distinct} distinct colours drawn from {render_nodes[0]}"
    finally:
        player.close()
        os.close(render_fd)
        make_current(display, None, None, None)
        destroy_context(display, context)
        terminate(display)


def loadable(soname: str) -> bool:
    try:
        ctypes.CDLL(soname)
        return True
    except OSError:
        return False


class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *_):
        pass


def serve(directory: Path) -> tuple[http.server.ThreadingHTTPServer, str]:
    handler = functools.partial(QuietHandler, directory=str(directory))
    server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server, f"http://127.0.0.1:{server.server_address[1]}"


def run_checks():
    results: list[str] = []
    failures: list[str] = []

    def check(name: str, run):
        try:
            outcome = run()
            results.append(f"{name}: {outcome}" if outcome else f"{name}: not available on this machine")
        except Exception as error:  # each failure is reported with its check, and the rest still run
            failures.append(f"{name}: {error}")

    version = Player()
    results.append(f"{version.get('mpv-version')}, FFmpeg {version.get('ffmpeg-version')}")
    devices = [device["name"] for device in json.loads(version.get("audio-device-list") or "[]")]
    version.close()

    server, root = serve(media)
    clip = str(media / "clip.mkv")
    try:
        def local_file():
            started = play(clip)
            if sorted(tracks(started)) != [("audio", "aac"), ("sub", "ass"), ("video", "h264")]:
                raise RuntimeError(f"tracks {tracks(started)}")
            return "H.264, AAC and ASS played to the end"

        def hls():
            started = play(f"{root}/hls/index.m3u8", cache="yes", **{"demuxer-max-bytes": "64MiB"})
            cache = json.loads(started["demuxer-cache-state"] or "{}")
            if started["file-format"] != "hls" or started["demuxer-via-network"] != "yes" or "cache-end" not in cache:
                raise RuntimeError(f"format {started['file-format']}, cache {cache}")
            return f"played over HTTP, demuxer cache up to {cache['cache-end']:.1f} s at the start"

        def dash():
            started = play(f"{root}/dash/manifest.mpd")
            if started["file-format"] != "dash" or ("video", "h264") not in tracks(started):
                raise RuntimeError(f"format {started['file-format']}, tracks {tracks(started)}")
            return "played over HTTP"

        def webvtt_in_mp4():
            started = play(clip, **{"sub-files": str(media / "wvtt.mp4")})
            if ("sub", "webvtt") not in tracks(started):
                raise RuntimeError(f"tracks {tracks(started)}: the fork's FFmpeg patch is missing")
            return "the WebVTT track is listed"

        check("local file", local_file)
        check("HLS", hls)
        check("DASH", dash)
        check("WebVTT in MP4", webvtt_in_mp4)
        check("subtitles through libass and the SW render API", lambda: check_subtitles_drawn(clip))

        def audio_outputs():
            if hidden:
                found = [device for device in devices if device.split("/")[0] in ("pipewire", "pulse", "alsa")]
                if found:
                    raise RuntimeError(f"listed {found} without the audio libraries")
                return "none listed, as expected"
            runtime = Path(os.environ.get("XDG_RUNTIME_DIR", f"/run/user/{os.getuid()}"))
            expected = {
                "pipewire": loadable("libpipewire-0.3.so.0") and (runtime / "pipewire-0").exists(),
                "pulse": loadable("libpulse.so.0") and (runtime / "pulse" / "native").exists(),
                "alsa": loadable("libasound.so.2"),
            }
            missing = [output for output, wanted in expected.items()
                       if wanted and not any(device.startswith(output + "/") for device in devices)]
            if missing:
                raise RuntimeError(f"no devices from {missing}; listed {devices}")
            counts = {output: sum(device.startswith(output + "/") for device in devices) for output in expected}
            return ", ".join(f"{count} {output}" for output, count in counts.items())

        check("audio devices", audio_outputs)

        def decoding(hwdec: str, available: bool):
            def run():
                if hidden:
                    started = play(clip, until_end=False, hwdec=hwdec)
                    if started["hwdec-current"] not in (None, "", "no"):
                        raise RuntimeError(f"decoded with {started['hwdec-current']} without its libraries")
                    return "fell back to software decoding"
                if not available:
                    return None
                started = play(clip, until_end=False, hwdec=hwdec)
                if started["hwdec-current"] != hwdec:
                    raise RuntimeError(f"hwdec-current is {started['hwdec-current']}")
                return f"hwdec-current {hwdec}"
            return run

        render_node = any(Path("/dev/dri").glob("renderD*"))
        check("VA-API decoding", decoding("vaapi-copy", render_node and loadable("libva-drm.so.2")))
        check("Vulkan decoding", decoding("vulkan-copy", loadable("libvulkan.so.1")))
        if not hidden:
            check("VA-API into OpenGL", lambda: check_vaapi_into_opengl(clip))
    finally:
        server.shutdown()

    print("\n".join(f"  {line}" for line in results), flush=True)
    if failures:
        sys.exit("checks failed:\n  " + "\n  ".join(failures))


def run_without_system_libraries():
    """Runs the checks again in a process where dlopen cannot find the system libraries."""
    with tempfile.TemporaryDirectory() as scratch:
        source, shim = Path(scratch) / "hide.c", Path(scratch) / "hide.so"
        source.write_text("""#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>

void *dlopen(const char *file, int mode)
{
    static void *(*real)(const char *, int);
    if (!real)
        real = (void *(*)(const char *, int)) dlsym(RTLD_NEXT, "dlopen");
    const char *hidden = getenv("HIDDEN_LIBRARIES");
    if (file && hidden) {
        size_t length = strlen(file);
        for (const char *at = strstr(hidden, file); at; at = strstr(at + 1, file)) {
            if ((at == hidden || at[-1] == ' ') && (at[length] == ' ' || at[length] == 0))
                return NULL;
        }
    }
    return real(file, mode);
}
""")
        subprocess.run([compiler, "-shared", "-fPIC", "-O2", str(source), "-o", str(shim), "-ldl"], check=True)
        environment = {**os.environ, "LD_PRELOAD": str(shim), "HIDDEN_LIBRARIES": " ".join(SYSTEM_LIBRARIES)}
        result = subprocess.run([sys.executable, "-I", __file__, HIDDEN_FLAG, *arguments], env=environment)
        if result.returncode:
            sys.exit("the library does not work without the system libraries it opens itself")


make_media()
print("without " + ", ".join(SYSTEM_LIBRARIES) + ":" if hidden else f"{library_path}:", flush=True)
run_checks()
if not hidden:
    run_without_system_libraries()
