import logo from "../site/katari.svg?raw";

export function GET(): Response {
  return new Response(logo, { headers: { "content-type": "image/svg+xml" } });
}
