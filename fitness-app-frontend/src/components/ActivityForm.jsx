import {
  Alert,
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { addActivity } from "../services/api";

const defaultActivity = {
  type: "RUNNING",
  duration: "",
  caloriesBurned: "",
  additionalMetrics: {},
};

const activityNotes = {
  RUNNING: "Use this for outdoor runs, treadmill sessions, and intervals.",
  WALKING: "Capture lighter recovery work, hikes, and daily walking blocks.",
  CYCLING: "Log road rides, indoor bike sessions, or endurance blocks.",
};

const ActivityForm = ({ onActivityAdded }) => {
  const [activity, setActivity] = useState(defaultActivity);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      await addActivity({
        ...activity,
        duration: Number(activity.duration),
        caloriesBurned: Number(activity.caloriesBurned),
      });
      setActivity(defaultActivity);
      onActivityAdded?.();
    } catch (submitError) {
      console.error(submitError);
      setError("Unable to save this activity right now.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      sx={{
        border: "1px solid",
        borderColor: "divider",
        borderRadius: 3,
        bgcolor: "rgba(255,255,255,0.84)",
        p: { xs: 2, md: 3 },
      }}
    >
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 0.75 }}>
        Log a workout
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Add the core session details now. Recommendation data appears after the
        activity is processed downstream.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Stack spacing={2}>
        <FormControl fullWidth>
          <InputLabel id="activity-type-label">Activity type</InputLabel>
          <Select
            labelId="activity-type-label"
            value={activity.type}
            label="Activity type"
            onChange={(event) =>
              setActivity({ ...activity, type: event.target.value })
            }
          >
            <MenuItem value="RUNNING">Running</MenuItem>
            <MenuItem value="WALKING">Walking</MenuItem>
            <MenuItem value="CYCLING">Cycling</MenuItem>
          </Select>
        </FormControl>

        <Typography variant="body2" color="text.secondary">
          {activityNotes[activity.type]}
        </Typography>

        <TextField
          fullWidth
          label="Duration"
          type="number"
          value={activity.duration}
          onChange={(event) =>
            setActivity({ ...activity, duration: event.target.value })
          }
          slotProps={{
            input: { inputProps: { min: 1 } },
          }}
          helperText="Minutes spent in the session"
        />

        <TextField
          fullWidth
          label="Calories burned"
          type="number"
          value={activity.caloriesBurned}
          onChange={(event) =>
            setActivity({ ...activity, caloriesBurned: event.target.value })
          }
          slotProps={{
            input: { inputProps: { min: 0 } },
          }}
          helperText="Estimated energy output"
        />

        <Button
          type="submit"
          variant="contained"
          size="large"
          disabled={submitting}
          sx={{ mt: 1, py: 1.4 }}
        >
          {submitting ? "Saving activity..." : "Save activity"}
        </Button>
      </Stack>
    </Box>
  );
};

export default ActivityForm;
