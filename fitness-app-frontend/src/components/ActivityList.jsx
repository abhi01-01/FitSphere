import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Skeleton,
  Stack,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router";

const formatActivityType = (type) =>
  type
    ?.toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase()) || "Unspecified";

const EmptyState = () => (
  <Box
    sx={{
      border: "1px dashed",
      borderColor: "divider",
      borderRadius: 2.5,
      py: 6,
      px: 3,
      textAlign: "center",
      bgcolor: "rgba(249,250,251,0.9)",
    }}
  >
    <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
      No workouts logged yet
    </Typography>
    <Typography variant="body2" color="text.secondary">
      Add your first session to start building a training history.
    </Typography>
  </Box>
);

const ActivitySkeletons = () => (
  <Stack spacing={2}>
    {Array.from({ length: 3 }).map((_, index) => (
      <Box
        key={index}
        sx={{
          border: "1px solid",
          borderColor: "divider",
          borderRadius: 2.5,
          p: 2.5,
        }}
      >
        <Skeleton variant="text" width="35%" height={34} />
        <Skeleton variant="text" width="55%" />
        <Skeleton variant="rounded" width="100%" height={56} sx={{ mt: 2 }} />
      </Box>
    ))}
  </Stack>
);

const ActivityList = ({ activities, loading }) => {
  const navigate = useNavigate();

  if (loading) {
    return <ActivitySkeletons />;
  }

  if (!activities.length) {
    return <EmptyState />;
  }

  return (
    <Stack spacing={2}>
      {activities.map((activity) => (
        <Card
          key={activity.id}
          variant="outlined"
          sx={{
            borderRadius: 2.5,
            borderColor: "rgba(17, 24, 39, 0.08)",
            boxShadow: "none",
            transition: "transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease",
            "&:hover": {
              transform: "translateY(-2px)",
              borderColor: "primary.main",
              boxShadow: "0 16px 28px rgba(17, 24, 39, 0.08)",
            },
          }}
        >
          <CardContent sx={{ p: "20px !important" }}>
            <Stack
              direction={{ xs: "column", md: "row" }}
              justifyContent="space-between"
              alignItems={{ xs: "flex-start", md: "center" }}
              spacing={2}
            >
              <Box sx={{ minWidth: 0 }}>
                <Stack direction="row" spacing={1} sx={{ mb: 1.25, flexWrap: "wrap" }}>
                  <Chip
                    label={formatActivityType(activity.type)}
                    color="primary"
                    size="small"
                    sx={{ borderRadius: 1.5 }}
                  />
                  <Chip
                    label={`${activity.duration} min`}
                    variant="outlined"
                    size="small"
                    sx={{ borderRadius: 1.5 }}
                  />
                </Stack>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 0.5 }}>
                  {activity.caloriesBurned} calories burned
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Open the session to inspect its AI-generated recommendation and
                  follow-up guidance.
                </Typography>
              </Box>

              <Button
                variant="outlined"
                onClick={() => navigate(`/activities/${activity.id}`)}
                sx={{ minWidth: 148, alignSelf: { xs: "stretch", md: "center" } }}
              >
                View details
              </Button>
            </Stack>
          </CardContent>
        </Card>
      ))}
    </Stack>
  );
};

export default ActivityList;
