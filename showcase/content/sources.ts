/**
 * Openly licensed works the showcase library is built from.
 *
 * `fetch.ts` resolves these against their upstream sites on every run and downloads what they name. What the showcase
 * shows changes when this file changes, or when an upstream site changes a file; the capture run's screenshots then
 * show the difference for review.
 *
 * A work's `kind` picks its resolver: `peppercarrot` lists episode pages on peppercarrot.com, `gutenberg` reads a
 * Project Gutenberg book's catalog record, and `film` takes its metadata as written here.
 */
import type { CoverDefinition, SourceDefinition, WorkDefinition } from "./source-definitions.ts";
import sourceList from "./source-list/sources.json" with { type: "json" };

// A file of its own, because the showcase extension lists the same sources before it can reach the content server.
export const sources = sourceList as SourceDefinition[];

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
  { id: "treasure-island", source: "gutenberg", kind: "gutenberg", book: 120 },
  { id: "peter-pan", source: "gutenberg", kind: "gutenberg", book: 16 },
  { id: "dorian-gray", source: "gutenberg", kind: "gutenberg", book: 174 },
  { id: "grimms-fairy-tales", source: "gutenberg", kind: "gutenberg", book: 2591 },
  { id: "moby-dick", source: "gutenberg", kind: "gutenberg", book: 2701 },
  { id: "war-of-the-worlds", source: "gutenberg", kind: "gutenberg", book: 36, title: "The War of the Worlds" },
  { id: "jekyll-and-hyde", source: "gutenberg", kind: "gutenberg", book: 43, title: "The Strange Case of Dr. Jekyll and Mr. Hyde" },
  { id: "wizard-of-oz", source: "gutenberg", kind: "gutenberg", book: 55 },
  { id: "huckleberry-finn", source: "gutenberg", kind: "gutenberg", book: 76 },
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
    // Wikimedia's 1080p transcode is a fifth the size of Blender's own 1080p file, for the same frames on a phone.
    video: {
      url: "https://upload.wikimedia.org/wikipedia/commons/transcoded/f/f1/Sintel_movie_4K.webm/Sintel_movie_4K.webm.1080p.vp9.webm",
    },
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
    // Only its cover is shown, so the smallest encode will do.
    video: {
      url: "https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4.zip",
      member: "BigBuckBunny_320x180.mp4",
    },
    cover: { url: "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_buck_bunny_poster_big.jpg" },
  },
];
