/**
 * Openly licensed works the showcase library is built from.
 *
 * `pin.ts` resolves these into `content.lock.json`: every file with its URL, size, and sha256, plus the metadata and
 * attribution each work needs. `fetch.ts` only ever reads the lock, so what the showcase shows changes only when this
 * file changes and the lock is pinned again.
 *
 * A work's `kind` picks its resolver: `peppercarrot` lists episode pages on peppercarrot.com, `gutenberg` reads a
 * Project Gutenberg book's catalog record, and `film` takes its metadata as written here.
 */
import type { CoverDefinition, SourceDefinition, WorkDefinition } from "./source-definitions.ts";

export const sources: SourceDefinition[] = [
  { id: "pepper-carrot-en", name: "Pepper&Carrot", language: "en", type: "manga" },
  { id: "pepper-carrot-ja", name: "Pepper&Carrot", language: "ja", type: "manga" },
  { id: "gutenberg", name: "Project Gutenberg", language: "all", type: "book" },
  { id: "blender-studio", name: "Blender Studio", language: "en", type: "anime" },
];

// The right half of the printed Book 1 wraparound cover is a portrait front cover.
const pepperCarrotCover: CoverDefinition = {
  url: "https://www.peppercarrot.com/0_sources/0ther/book-publishing/low-res/en_B01_cover_softcover_72pages_8.5x11_by-David-Revoy.jpg",
  crop: { left: 846, top: 0, width: 813, height: 1080 },
};

export const works: WorkDefinition[] = [
  {
    id: "pepper-carrot-en",
    source: "pepper-carrot-en",
    kind: "peppercarrot",
    language: "en",
    episodes: { first: 1, last: 8 },
    title: "Pepper&Carrot",
    description:
      "Pepper, a young witch of Chaosah, and her cat Carrot stumble from one magical mishap to the next in the world of Hereva.",
    genres: ["Fantasy", "Comedy", "Adventure"],
    cover: pepperCarrotCover,
  },
  {
    id: "pepper-carrot-ja",
    source: "pepper-carrot-ja",
    kind: "peppercarrot",
    language: "ja",
    episodes: { first: 1, last: 8 },
    title: "ペッパー＆キャロット",
    description: "カオサの見習い魔女ペッパーと相棒の猫キャロットが、魔法の世界エレヴァで騒動を巻き起こす。",
    genres: ["Fantasy", "Comedy", "Adventure"],
    cover: pepperCarrotCover,
  },
  { id: "alice-in-wonderland", source: "gutenberg", kind: "gutenberg", book: 11 },
  { id: "pride-and-prejudice", source: "gutenberg", kind: "gutenberg", book: 1342 },
  {
    id: "frankenstein",
    source: "gutenberg",
    kind: "gutenberg",
    book: 84,
    title: "Frankenstein; or, The Modern Prometheus",
  },
  { id: "the-time-machine", source: "gutenberg", kind: "gutenberg", book: 35 },
  { id: "sherlock-holmes", source: "gutenberg", kind: "gutenberg", book: 1661 },
  {
    id: "rashomon",
    source: "gutenberg",
    kind: "gutenberg",
    book: 1982,
    // Gutenberg's cover for this book is a generated placeholder. This print by Tsukioka Yoshitoshi shows an old
    // woman with long white hair, as in Akutagawa's story.
    cover: {
      url: "https://upload.wikimedia.org/wikipedia/commons/1/1d/The_Demon_of_Rashomon_Visits_Watanabe_no_Tsuna_Disguised_as_an_Old_Woman_to_Retrieve_Her_Severed_Arm_LACMA_M.84.31.451a-b.jpg",
      crop: { left: 60, top: 40, width: 1010, height: 1515 },
      credit: "Tsukioka Yoshitoshi, Los Angeles County Museum of Art, public domain",
    },
  },
  {
    id: "sintel",
    source: "blender-studio",
    kind: "film",
    title: "Sintel",
    author: "Blender Foundation",
    artist: "Colin Levy",
    released: "2010-09-27",
    description:
      "A lonely young woman searches the wilderness for the baby dragon she once rescued, and pays dearly for finding it.",
    genres: ["Fantasy", "Adventure", "Drama"],
    license: { name: "CC BY 3.0", url: "https://creativecommons.org/licenses/by/3.0/" },
    attribution: "© copyright Blender Foundation | durian.blender.org",
    video: { url: "https://download.blender.org/demo/movies/Sintel.2010.1080p.mkv" },
    cover: { url: "https://upload.wikimedia.org/wikipedia/commons/8/8f/Sintel_poster.jpg" },
  },
  {
    id: "big-buck-bunny",
    source: "blender-studio",
    kind: "film",
    title: "Big Buck Bunny",
    author: "Blender Foundation",
    artist: "Sacha Goedegebure",
    released: "2008-05-30",
    description: "A gentle giant of a rabbit has had enough of three rodents who bully the forest's smaller creatures.",
    genres: ["Comedy", "Family"],
    license: { name: "CC BY 3.0", url: "https://creativecommons.org/licenses/by/3.0/" },
    attribution: "© copyright Blender Foundation | www.bigbuckbunny.org",
    video: {
      url: "https://download.blender.org/demo/movies/BBB/bbb_sunflower_1080p_30fps_normal.mp4.zip",
      member: "bbb_sunflower_1080p_30fps_normal.mp4",
    },
    cover: { url: "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_buck_bunny_poster_big.jpg" },
  },
];
