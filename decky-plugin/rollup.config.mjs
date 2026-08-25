import { defineConfig } from "rollup";
import deckyPlugin from "@decky/rollup";

export default defineConfig({
  input: "src/index.tsx",
  plugins: [
    deckyPlugin()
  ]
});
