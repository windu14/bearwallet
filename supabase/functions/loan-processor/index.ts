// Supabase Edge Function: loan-processor
// Processes pending loan stages automatically if running background workers.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.38.4"

serve(async (req) => {
  try {
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const { applicationId } = await req.json()

    if (!applicationId) {
      return new Response(JSON.stringify({ error: "applicationId is required" }), { status: 400 })
    }

    // Advance loan stage
    const { data, error } = await supabaseClient.rpc('process_loan_stage', {
      p_application_id: applicationId
    })

    if (error) throw error

    return new Response(JSON.stringify({ success: true, stage_result: data }), {
      headers: { "Content-Type": "application/json" },
      status: 200,
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { "Content-Type": "application/json" },
      status: 500,
    })
  }
})
