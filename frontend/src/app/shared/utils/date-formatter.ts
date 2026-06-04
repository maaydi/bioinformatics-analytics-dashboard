/** Format a date string (ISO 8601) to human-readable format. */
export const formatDate = (dateStr: string | null | undefined): string => {
  return dateStr ? new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  }) : '–';
};
