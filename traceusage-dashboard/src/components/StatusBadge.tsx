type Tone = "success" | "warning" | "danger" | "info" | "neutral" | "purple";

const STATUS_TONES: Record<string, Tone> = {
  ACCESS_OBSERVED: "success",
  NO_ACCESS_OBSERVED: "warning",
  DEPRECATED_NO_ACCESS_OBSERVED: "danger",
  NEWLY_MONITORED: "neutral",
  ACTIVE: "success",
  INACTIVE: "warning",
  DEPRECATED_ACTIVE: "danger",
  DEPRECATED_INACTIVE: "purple",
  REMOVAL_CANDIDATE: "danger",
  IMPORTED: "success",
  ALREADY_IMPORTED: "info",
};

const STATUS_LABELS: Record<string, string> = {
  ACCESS_OBSERVED: "Access observed",
  NO_ACCESS_OBSERVED: "No access observed",
  DEPRECATED_NO_ACCESS_OBSERVED: "Deprecated, no access observed",
  NEWLY_MONITORED: "Newly monitored",
  ACTIVE: "Active",
  INACTIVE: "Inactive",
  DEPRECATED_ACTIVE: "Deprecated, still used",
  DEPRECATED_INACTIVE: "Deprecated, inactive",
  REMOVAL_CANDIDATE: "Manual review candidate",
  IMPORTED: "Imported",
  ALREADY_IMPORTED: "Already imported",
};

export function StatusBadge({ status, label }: { status: string; label?: string }) {
  const tone = STATUS_TONES[status] ?? "neutral";
  return <span className={`badge ${tone}`}>{label ?? STATUS_LABELS[status] ?? status}</span>;
}
