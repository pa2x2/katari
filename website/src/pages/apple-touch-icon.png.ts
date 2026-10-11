import { Resvg } from "@resvg/resvg-js";
import logo from "../site/katari.svg?raw";

export function GET(): Response {
  // iOS rounds the corners itself and fills whatever the logo leaves transparent with black, so the field goes square.
  const square = logo.replace(' rx="30"', "");
  const png = new Resvg(square, { fitTo: { mode: "width", value: 180 } }).render().asPng();
  return new Response(new Uint8Array(png), { headers: { "content-type": "image/png" } });
}
