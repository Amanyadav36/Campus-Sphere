// Supabase Edge Function: campus-ai (RAG-aware)
import { serve } from "https://deno.land/std@0.224.0/http/server.ts";

const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function buildPrompt(question: string, data: string): string {
  return `You are a campus assistant.

Answer ONLY using the provided DATA. Do not use any outside knowledge.

If the answer is not present in DATA, reply exactly:
No relevant data found.

When the question is about an event, include these fields if present in DATA:
Title, Club, Date, Time, Venue, Category, Price, Description.

DATA:
${data || ""}

QUESTION:
${question}

ANSWER:`;
}

async function callGemini(prompt: string): Promise<string> {
  const apiKey = Deno.env.get("GEMINI_API_KEY");
  if (!apiKey) throw new Error("Missing GEMINI_API_KEY");

  const model = Deno.env.get("GEMINI_MODEL") || "gemini-1.5-flash";
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

  const resp = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [{ role: "user", parts: [{ text: prompt }] }],
      generationConfig: { temperature: 0.0, topP: 0.1, maxOutputTokens: 350 },
    }),
  });

  const json = await resp.json();
  if (!resp.ok) throw new Error(`Gemini error: ${JSON.stringify(json)}`);

  return json?.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "No relevant data found.";
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

  try {
    const body = await req.json().catch(() => ({}));

    const question = body.question || body.message || "";
    const data = body.data || "";

    // If the client didn't send any context, never let the model guess.
    if (!data || String(data).trim().length < 10) {
      return new Response(JSON.stringify({ reply: "No relevant data found." }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    const prompt = buildPrompt(question, data);
    const reply = await callGemini(prompt);

    return new Response(JSON.stringify({ reply }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 200,
    });
  } catch (e) {
    console.error("Error:", e.message);
    return new Response(JSON.stringify({ error: e.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});