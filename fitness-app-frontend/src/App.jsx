import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  LinearProgress,
  Stack,
  Typography,
} from "@mui/material";
import { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { AuthContext } from "react-oauth2-code-pkce";
import { useDispatch } from "react-redux";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router";
import ActivityDetail from "./components/ActivityDetail";
import ActivityForm from "./components/ActivityForm";
import ActivityList from "./components/ActivityList";
import { getActivities } from "./services/api";
import { logout, setCredentials } from "./store/authSlice";

const formatActivityType = (type) =>
  type
    ?.toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase()) || "Unspecified";

const buildActivitySummary = (activities) => {
  const totalMinutes = activities.reduce(
    (sum, activity) => sum + Number(activity.duration || 0),
    0
  );
  const totalCalories = activities.reduce(
    (sum, activity) => sum + Number(activity.caloriesBurned || 0),
    0
  );

  const activityMix = activities.reduce((counts, activity) => {
    const key = activity.type || "OTHER";
    counts[key] = (counts[key] || 0) + 1;
    return counts;
  }, {});

  const topType = Object.entries(activityMix).sort((a, b) => b[1] - a[1])[0];

  return [
    {
      label: "Sessions logged",
      value: activities.length,
      helper: activities.length
        ? "Recent activity history"
        : "Start your first workout log",
    },
    {
      label: "Minutes trained",
      value: totalMinutes,
      helper: "Total recorded duration",
    },
    {
      label: "Calories burned",
      value: totalCalories,
      helper: "Aggregate session output",
    },
    {
      label: "Most frequent",
      value: topType ? formatActivityType(topType[0]) : "None yet",
      helper: topType ? `${topType[1]} sessions logged` : "No activity mix yet",
    },
  ];
};

const SummaryStrip = ({ activities }) => {
  const summary = useMemo(() => buildActivitySummary(activities), [activities]);

  return (
    <Box
      sx={{
        display: "grid",
        gridTemplateColumns: {
          xs: "1fr",
          sm: "repeat(2, minmax(0, 1fr))",
          xl: "repeat(4, minmax(0, 1fr))",
        },
        gap: 2,
      }}
    >
      {summary.map((item) => (
        <Box
          key={item.label}
          sx={{
            border: "1px solid",
            borderColor: "divider",
            borderRadius: 2,
            p: 2.5,
            minHeight: 132,
            bgcolor: "rgba(255,255,255,0.72)",
            backdropFilter: "blur(10px)",
          }}
        >
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            {item.label}
          </Typography>
          <Typography
            variant="h4"
            sx={{
              mb: 1,
              fontWeight: 700,
              color: "text.primary",
              fontSize: { xs: "1.8rem", md: "2.1rem" },
            }}
          >
            {item.value}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {item.helper}
          </Typography>
        </Box>
      ))}
    </Box>
  );
};

const DashboardPage = ({
  activities,
  loading,
  error,
  onRefresh,
  onActivityAdded,
  userName,
}) => (
  <Stack spacing={3}>
    <Box
      sx={{
        display: "grid",
        gap: 3,
        gridTemplateColumns: { xs: "1fr", xl: "380px minmax(0, 1fr)" },
        alignItems: "start",
      }}
    >
      <ActivityForm onActivityAdded={onActivityAdded} />

      <Box
        sx={{
          border: "1px solid",
          borderColor: "divider",
          borderRadius: 3,
          bgcolor: "rgba(250, 251, 252, 0.78)",
          overflow: "hidden",
        }}
      >
        <Box
          sx={{
            px: { xs: 2, md: 3 },
            py: 2.5,
            display: "flex",
            justifyContent: "space-between",
            alignItems: { xs: "flex-start", md: "center" },
            gap: 2,
            flexDirection: { xs: "column", md: "row" },
          }}
        >
          <Box>
            <Typography variant="h5" sx={{ fontWeight: 700, mb: 0.5 }}>
              {userName ? `${userName}'s training log` : "Training log"}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Review recent sessions and open AI guidance for each workout.
            </Typography>
          </Box>
          <Button
            variant="outlined"
            onClick={onRefresh}
            sx={{ minWidth: 132, alignSelf: { xs: "stretch", md: "auto" } }}
          >
            Refresh
          </Button>
        </Box>

        <Divider />

        {loading && <LinearProgress />}

        <Box sx={{ px: { xs: 2, md: 3 }, py: 2.5 }}>
          {error && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <ActivityList activities={activities} loading={loading} />
        </Box>
      </Box>
    </Box>
  </Stack>
);

const LoginScreen = ({ onLogin }) => (
  <Box
    sx={{
      minHeight: "100vh",
      display: "grid",
      placeItems: "center",
      px: 2,
      py: 4,
      background:
        "linear-gradient(145deg, rgba(237,245,241,0.98) 0%, rgba(255,248,238,0.98) 48%, rgba(246,247,252,0.98) 100%), url('https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1600&q=80') center/cover",
      backgroundBlendMode: "soft-light",
    }}
  >
    <Box
      sx={{
        width: "100%",
        maxWidth: 1120,
        display: "grid",
        gap: 3,
        gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1.2fr) 420px" },
        alignItems: "stretch",
      }}
    >
      <Box
        sx={{
          minHeight: { xs: 320, lg: 620 },
          borderRadius: 3,
          overflow: "hidden",
          position: "relative",
          backgroundImage:
            "linear-gradient(180deg, rgba(14,24,19,0.1), rgba(14,24,19,0.68)), url('https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=1600&q=80')",
          backgroundSize: "cover",
          backgroundPosition: "center",
          display: "flex",
          alignItems: "flex-end",
          p: { xs: 3, md: 5 },
          color: "#f7faf8",
        }}
      >
        <Box sx={{ maxWidth: 520 }}>
          <Chip
            label="Performance tracking"
            sx={{
              mb: 2,
              bgcolor: "rgba(255,255,255,0.12)",
              color: "inherit",
              borderRadius: 1.5,
            }}
          />
          <Typography
            variant="h2"
            sx={{
              fontWeight: 800,
              lineHeight: 1,
              mb: 2,
              fontSize: { xs: "2.4rem", md: "4rem" },
            }}
          >
            FitSphere
          </Typography>
          <Typography
            variant="h6"
            sx={{
              maxWidth: 460,
              color: "rgba(247,250,248,0.84)",
              fontWeight: 400,
            }}
          >
            Track every session, review workload trends, and pull AI-backed
            recovery guidance from one focused workspace.
          </Typography>
        </Box>
      </Box>

      <Box
        sx={{
          borderRadius: 3,
          border: "1px solid",
          borderColor: "rgba(34, 52, 43, 0.12)",
          bgcolor: "rgba(255,255,255,0.82)",
          backdropFilter: "blur(18px)",
          p: { xs: 3, md: 4 },
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          gap: 4,
        }}
      >
        <Box>
          <Typography
            variant="overline"
            sx={{ color: "text.secondary", letterSpacing: 0, fontWeight: 600 }}
          >
            Sign in
          </Typography>
          <Typography variant="h4" sx={{ mt: 0.5, mb: 1.5, fontWeight: 700 }}>
            Continue to your activity dashboard
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Use your Keycloak account to open the workout tracker and
            recommendation feed.
          </Typography>
        </Box>

        <Stack spacing={1.5}>
          {[
            "Structured workout logging",
            "Per-session recommendation detail",
            "Fast review of duration, calories, and activity mix",
          ].map((item) => (
            <Box
              key={item}
              sx={{
                px: 2,
                py: 1.5,
                borderRadius: 2,
                bgcolor: "rgba(242, 246, 244, 0.9)",
                border: "1px solid",
                borderColor: "rgba(34, 52, 43, 0.08)",
              }}
            >
              <Typography variant="body2">{item}</Typography>
            </Box>
          ))}
        </Stack>

        <Box>
          <Button
            variant="contained"
            size="large"
            onClick={onLogin}
            fullWidth
            sx={{ py: 1.5 }}
          >
            Continue with Keycloak
          </Button>
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ mt: 1.5, textAlign: "center" }}
          >
            Access is restricted to your configured development realm.
          </Typography>
        </Box>
      </Box>
    </Box>
  </Box>
);

function App() {
  const { token, tokenData, logIn, logOut } = useContext(AuthContext);
  const dispatch = useDispatch();
  const [activities, setActivities] = useState([]);
  const [activitiesLoading, setActivitiesLoading] = useState(false);
  const [activitiesError, setActivitiesError] = useState("");

  useEffect(() => {
    if (token) {
      dispatch(setCredentials({ token, user: tokenData }));
    } else {
      dispatch(logout());
    }
  }, [token, tokenData, dispatch]);

  const handleLogin = useCallback(() => {
    logIn(undefined, { prompt: "login" });
  }, [logIn]);

  const handleLogout = useCallback(() => {
    dispatch(logout());
    setActivities([]);
    setActivitiesError("");
    logOut();
  }, [dispatch, logOut]);

  const fetchActivities = useCallback(async () => {
    if (!token) {
      setActivities([]);
      return;
    }

    setActivitiesLoading(true);
    setActivitiesError("");

    try {
      const response = await getActivities();
      setActivities(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error(error);
      setActivitiesError("Unable to load activities right now.");
    } finally {
      setActivitiesLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchActivities();
  }, [fetchActivities]);

  const displayName =
    tokenData?.preferred_username ||
    tokenData?.name ||
    tokenData?.email ||
    "Athlete";

  return (
    <Router>
      {!token ? (
        <LoginScreen onLogin={handleLogin} />
      ) : (
        <Box
          sx={{
            minHeight: "100vh",
            bgcolor: "#f3f6f4",
            color: "text.primary",
          }}
        >
          <Box
            component="header"
            sx={{
              borderBottom: "1px solid",
              borderColor: "divider",
              bgcolor: "rgba(255,255,255,0.82)",
              backdropFilter: "blur(14px)",
              position: "sticky",
              top: 0,
              zIndex: 10,
            }}
          >
            <Box
              sx={{
                maxWidth: 1240,
                mx: "auto",
                px: { xs: 2, md: 4 },
                py: 2,
                display: "flex",
                justifyContent: "space-between",
                alignItems: { xs: "flex-start", md: "center" },
                flexDirection: { xs: "column", md: "row" },
                gap: 2,
              }}
            >
              <Box>
                <Typography variant="h5" sx={{ fontWeight: 800 }}>
                  FitSphere
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Activity tracking and AI-led recovery guidance
                </Typography>
              </Box>

              <Stack
                direction={{ xs: "column", sm: "row" }}
                spacing={1.5}
                alignItems={{ xs: "stretch", sm: "center" }}
              >
                <Chip
                  label={displayName}
                  variant="outlined"
                  sx={{ borderRadius: 1.5, px: 0.75 }}
                />
                <Button variant="outlined" color="inherit" onClick={handleLogout}>
                  Sign out
                </Button>
              </Stack>
            </Box>
          </Box>

          <Box sx={{ maxWidth: 1240, mx: "auto", px: { xs: 2, md: 4 }, py: 4 }}>
            <Stack spacing={3}>
              <Box
                sx={{
                  display: "grid",
                  gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1.3fr) 320px" },
                  gap: 3,
                  alignItems: "end",
                }}
              >
                <Box>
                  <Typography
                    variant="h3"
                    sx={{
                      fontWeight: 800,
                      lineHeight: 1.02,
                      mb: 1,
                      fontSize: { xs: "2rem", md: "3.25rem" },
                    }}
                  >
                    Train with a clearer view of your workload.
                  </Typography>
                  <Typography
                    variant="body1"
                    color="text.secondary"
                    sx={{ maxWidth: 760 }}
                  >
                    Log sessions quickly, monitor volume across activities, and
                    inspect AI recommendations when a workout needs follow-up.
                  </Typography>
                </Box>

                <Box
                  sx={{
                    px: 2.5,
                    py: 2,
                    borderRadius: 2.5,
                    border: "1px solid",
                    borderColor: "divider",
                    bgcolor: "rgba(255,255,255,0.76)",
                  }}
                >
                  <Typography variant="body2" color="text.secondary">
                    Active realm
                  </Typography>
                  <Typography variant="h6" sx={{ mt: 0.5, fontWeight: 700 }}>
                    {import.meta.env.VITE_KEYCLOAK_REALM || "fitSphere-auth"}
                  </Typography>
                </Box>
              </Box>

              <SummaryStrip activities={activities} />

              <Routes>
                <Route
                  path="/activities"
                  element={
                    <DashboardPage
                      activities={activities}
                      loading={activitiesLoading}
                      error={activitiesError}
                      onRefresh={fetchActivities}
                      onActivityAdded={fetchActivities}
                      userName={displayName}
                    />
                  }
                />
                <Route
                  path="/activities/:id"
                  element={<ActivityDetail />}
                />
                <Route
                  path="/"
                  element={<Navigate to="/activities" replace />}
                />
              </Routes>
            </Stack>
          </Box>
        </Box>
      )}
    </Router>
  );
}

export default App;
