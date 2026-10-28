"use client";
import { createTheme, ThemeProvider } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import { AppRouterCacheProvider } from "@mui/material-nextjs/v15-appRouter";
import { ReactNode } from "react";
const theme = createTheme({
  palette: {
    primary: { main: "#c34e2b" },
    secondary: { main: "#252820" },
    background: { default: "#fcfbf8", paper: "#ffffff" },
    text: { primary: "#252820", secondary: "#72736b" },
  },
  typography: {
    fontFamily: "Arial, Helvetica, sans-serif",
    button: { textTransform: "none", fontWeight: 600 },
    h4: { fontWeight: 600, letterSpacing: "-0.04em" },
    h6: { fontWeight: 600 },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { borderRadius: 6, padding: "10px 20px" } },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: "none",
          boxShadow: "none",
          border: "1px solid #e8e8df",
        },
      },
    },
    MuiTextField: { defaultProps: { variant: "outlined" } },
  },
});
export default function ThemeRegistry({ children }: { children: ReactNode }) {
  return (
    <AppRouterCacheProvider>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </AppRouterCacheProvider>
  );
}
