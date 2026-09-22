export default function Pagination({ page, totalPages, onPageChange }) {
  if (!totalPages || totalPages <= 1) return null;

  const canPrev = page > 0;
  const canNext = page < totalPages - 1;

  return (
    <div className="flex items-center justify-center gap-3 pt-4">
      <button
        onClick={() => canPrev && onPageChange(page - 1)}
        disabled={!canPrev}
        className="rounded-full border border-ink/20 px-4 py-2 text-sm font-semibold text-ink disabled:opacity-40"
      >
        Anterior
      </button>
      <span className="text-sm text-ink/70">
        Página {page + 1} de {totalPages}
      </span>
      <button
        onClick={() => canNext && onPageChange(page + 1)}
        disabled={!canNext}
        className="rounded-full border border-ink/20 px-4 py-2 text-sm font-semibold text-ink disabled:opacity-40"
      >
        Siguiente
      </button>
    </div>
  );
}
