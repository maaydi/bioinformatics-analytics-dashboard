-- Read-only pre/post-import row counts for the current legacy public catalog.
-- Run against the same database snapshot before and after the benchmark.
SELECT 'protein_entry' AS object_name, COUNT(*) AS row_count FROM public.protein_entry
UNION ALL SELECT 'keyword', COUNT(*) FROM public.keyword
UNION ALL SELECT 'go_term', COUNT(*) FROM public.go_term
UNION ALL SELECT 'protein_keyword', COUNT(*) FROM public.protein_keyword
UNION ALL SELECT 'protein_go_term', COUNT(*) FROM public.protein_go_term
UNION ALL SELECT 'protein_feature', COUNT(*) FROM public.protein_feature
UNION ALL SELECT 'host_organism', COUNT(*) FROM public.host_organism
UNION ALL SELECT 'cross_reference', COUNT(*) FROM public.cross_reference
UNION ALL SELECT 'protein_comment', COUNT(*) FROM public.protein_comment
UNION ALL SELECT 'protein_publication', COUNT(*) FROM public.protein_publication
UNION ALL SELECT 'saved_filter', COUNT(*) FROM public.saved_filter
UNION ALL SELECT 'mv_dashboard_kpis', COUNT(*) FROM public.mv_dashboard_kpis
UNION ALL SELECT 'mv_reviewed_ratio', COUNT(*) FROM public.mv_reviewed_ratio
UNION ALL SELECT 'mv_evidence_distribution', COUNT(*) FROM public.mv_evidence_distribution
UNION ALL SELECT 'mv_length_histogram', COUNT(*) FROM public.mv_length_histogram
UNION ALL SELECT 'mv_organism_counts', COUNT(*) FROM public.mv_organism_counts
UNION ALL SELECT 'mv_keyword_frequency', COUNT(*) FROM public.mv_keyword_frequency
ORDER BY object_name;