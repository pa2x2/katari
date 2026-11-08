// The showcase extension's CONTENT_SERVER build field repeats this address.

export const CONTENT_SERVER_PORT = 8571;

/** The content server as the emulator reaches it: 10.0.2.2 is the host's loopback interface. */
export const EMULATOR_CONTENT_SERVER = `http://10.0.2.2:${CONTENT_SERVER_PORT}`;

/** Lists, in the folder fetch.ts builds, the files served from their upstream sites by path. */
export const STREAMED_FILES = "streamed.json";
