import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  Skeleton,
  Stack,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { getActivityDetail } from "../services/api";

const formatActivityType = (type) =>
  type
    ?.toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase()) || "Unspecified";

const RecommendationSection = ({ title, items }) => (
  <Box
    sx={{
      border: "1px solid",
      borderColor: "divider",
      borderRadius: 2.5,
      p: 2.5,
      bgcolor: "rgba(255,255,255,0.74)",
    }}
  >
    <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
      {title}
    </Typography>

    {items?.length ? (
      <Stack spacing={1.25}>
        {items.map((item, index) => (
          <Box
            key={`${title}-${index}`}
            sx={{
              px: 1.5,
              py: 1.25,
              borderRadius: 2,
              bgcolor: "rgba(246,247,248,0.92)",
            }}
          >
            <Typography variant="body2">{item}</Typography>
          </Box>
        ))}
      </Stack>
    ) : (
      <Typography variant="body2" color="text.secondary">
        No items available.
      </Typography>
    )}
  </Box>
);

const ActivityDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [recommendation, setRecommendation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchActivityDetail = async () => {
      setLoading(true);
      setError("");

      try {
        const response = await getActivityDetail(id);
        setRecommendation(response.data);
      } catch (requestError) {
        console.error(requestError);
        if (requestError.response?.status === 404) {
          setError(
            "No recommendation is available for this workout yet. The activity may still be processing."
          );
        } else {
          setError("Unable to load this recommendation right now.");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchActivityDetail();
  }, [id]);

  if (loading) {
    return (
      <Stack spacing={2}>
        <Skeleton variant="rounded" height={156} />
        <Skeleton variant="rounded" height={220} />
        <Skeleton variant="rounded" height={220} />
      </Stack>
    );
  }

  if (error) {
    return (
      <Box
        sx={{
          border: "1px solid",
          borderColor: "divider",
          borderRadius: 3,
          p: { xs: 2.5, md: 3 },
          bgcolor: "rgba(255,255,255,0.82)",
        }}
      >
        <Stack spacing={2}>
          <Button
            variant="text"
            sx={{ alignSelf: "flex-start", px: 0 }}
            onClick={() => navigate("/activities")}
          >
            Back to activities
          </Button>
          <Alert severity="info">{error}</Alert>
        </Stack>
      </Box>
    );
  }

  return (
    <Stack spacing={3}>
      <Button
        variant="text"
        sx={{ alignSelf: "flex-start", px: 0 }}
        onClick={() => navigate("/activities")}
      >
        Back to activities
      </Button>

      <Box
        sx={{
          border: "1px solid",
          borderColor: "divider",
          borderRadius: 3,
          p: { xs: 2.5, md: 3 },
          bgcolor: "rgba(255,255,255,0.82)",
        }}
      >
        <Stack spacing={2.5}>
          <Box>
            <Stack direction="row" spacing={1} sx={{ mb: 1.5, flexWrap: "wrap" }}>
              <Chip
                label={formatActivityType(recommendation.activityType)}
                color="primary"
                sx={{ borderRadius: 1.5 }}
              />
              <Chip
                label={new Date(recommendation.createdAt).toLocaleString()}
                variant="outlined"
                sx={{ borderRadius: 1.5 }}
              />
            </Stack>
            <Typography variant="h4" sx={{ fontWeight: 800, mb: 1 }}>
              Recommendation details
            </Typography>
            <Typography variant="body1" color="text.secondary">
              Workout ID: {recommendation.activityId}
            </Typography>
          </Box>

          <Divider />

          <Box
            sx={{
              borderRadius: 2.5,
              p: 2.5,
              bgcolor: "rgba(240,245,242,0.85)",
            }}
          >
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 1.25 }}>
              Analysis
            </Typography>
            <Typography variant="body1" sx={{ whiteSpace: "pre-wrap" }}>
              {recommendation.recommendation}
            </Typography>
          </Box>
        </Stack>
      </Box>

      <Box
        sx={{
          display: "grid",
          gap: 2,
          gridTemplateColumns: { xs: "1fr", lg: "repeat(3, minmax(0, 1fr))" },
        }}
      >
        <RecommendationSection
          title="Improvements"
          items={recommendation.improvements}
        />
        <RecommendationSection
          title="Suggestions"
          items={recommendation.suggestions}
        />
        <RecommendationSection
          title="Safety guidelines"
          items={recommendation.safety}
        />
      </Box>
    </Stack>
  );
};

export default ActivityDetail;
