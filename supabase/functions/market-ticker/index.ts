// Supabase Edge Function: market-ticker
// Invoked on a cron schedule (every 4 hours or 2 hours) to update Bear exchange rates.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.38.4"

serve(async (req) => {
  try {
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // Call server-side update_market_rate
    const { data, error } = await supabaseClient.rpc('update_market_rate', {
      p_market_event: 'Automated 4H Market Settlement'
    })

    if (error) throw error

    return new Response(JSON.stringify({ success: true, result: data }), {
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
