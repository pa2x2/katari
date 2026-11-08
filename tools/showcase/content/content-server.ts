// The showcase extension's CONTENT_SERVER build field repeats this address.

export const CONTENT_SERVER_PORT = 8571;

/**
 * The content server as both apps reach it: on the host's loopback interface, where the desktop app runs, and where
 * `adb reverse` forwards the emulator's own loopback port. One address keeps the seed's cover addresses and the
 * extension the same on both.
 */
export const CONTENT_SERVER = `http://127.0.0.1:${CONTENT_SERVER_PORT}`;

/** Lists, in the folder fetch.ts builds, the files served from their upstream sites by path. */
export const STREAMED_FILES = "streamed.json";
