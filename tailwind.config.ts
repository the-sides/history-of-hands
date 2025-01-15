import { type Config } from "tailwindcss";
import { fontFamily } from "tailwindcss/defaultTheme";

export default {
  content: ["./src/**/*.tsx"],
  theme: {
    extend: {
      fontFamily: {
        sans: [`"Jersey 10"`, ...fontFamily.sans],
      },
      fontSize: {
        'display1': '7rem'
      }
    },
  },
  plugins: [],
} satisfies Config;
