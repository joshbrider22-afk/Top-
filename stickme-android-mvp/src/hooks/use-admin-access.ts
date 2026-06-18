import { useEffect, useState } from "react";
import { supabase } from "../lib/supabase";

export type AdminAccessState = {
  loading: boolean;
  isAdmin: boolean;
};

export function useAdminAccess(): AdminAccessState {
  const [state, setState] = useState<AdminAccessState>({
    loading: true,
    isAdmin: false,
  });

  useEffect(() => {
    let mounted = true;

    async function load(): Promise<void> {
      const { data: userData } = await supabase.auth.getUser();

      if (!userData.user) {
        if (mounted) setState({ loading: false, isAdmin: false });
        return;
      }

      const { data, error } = await supabase
        .from("profiles")
        .select("role")
        .eq("id", userData.user.id)
        .maybeSingle();

      if (mounted) {
        setState({
          loading: false,
          isAdmin: !error && data?.role === "admin",
        });
      }
    }

    void load();

    return () => {
      mounted = false;
    };
  }, []);

  return state;
}
