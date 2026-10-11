import { renderPreviewCard } from "../social/preview-card.ts";

export async function GET(): Promise<Response> {
  return new Response(await renderPreviewCard(), { headers: { "content-type": "image/png" } });
}
