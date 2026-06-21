import React from "react";
import ReactDOM from "react-dom/client";
import { CssBaseline, ThemeProvider, createTheme } from "@mui/material";
import { AuthProvider } from "react-oauth2-code-pkce";
import { Provider } from "react-redux";
import App from "./App";
import { authConfig } from "./authConfig";
import "./index.css";
import { store } from "./store/store";

const theme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#206a4b",
      light: "#e8f4ee",
      dark: "#174f38",
    },
    secondary: {
      main: "#dc6c2e",
      light: "#fff0e7",
      dark: "#b5541e",
    },
    background: {
      default: "#f3f6f4",
      paper: "#ffffff",
    },
    text: {
      primary: "#13221b",
      secondary: "#5a675f",
    },
    divider: "rgba(19, 34, 27, 0.08)",
  },
  shape: {
    borderRadius: 8,
  },
  typography: {
    fontFamily:
      "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif",
    h2: {
      letterSpacing: 0,
    },
    h3: {
      letterSpacing: 0,
    },
    h4: {
      letterSpacing: 0,
    },
    h5: {
      letterSpacing: 0,
    },
    h6: {
      letterSpacing: 0,
    },
    button: {
      textTransform: "none",
      fontWeight: 600,
      letterSpacing: 0,
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          boxShadow: "none",
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 6,
        },
      },
    },
  },
});

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(
  <ThemeProvider theme={theme}>
    <CssBaseline />
    <AuthProvider authConfig={authConfig} loadingComponent={<div>Loading...</div>}>
      <Provider store={store}>
        <App />
      </Provider>
    </AuthProvider>
  </ThemeProvider>
);
