interface TablePaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

/**
 * Pagination numérotée compacte : première/dernière page, voisins,
 * ellipses (« … ») au lieu de lister tous les numéros.
 */
export function TablePagination({ page, totalPages, onPageChange }: TablePaginationProps) {
  if (totalPages <= 1) return null

  const numeros: (number | '…')[] = []
  for (let i = 0; i < totalPages; i++) {
    const visible = i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1
    if (visible) {
      numeros.push(i)
    } else if (numeros[numeros.length - 1] !== '…') {
      numeros.push('…')
    }
  }

  return (
    <div className="pagination">
      <button
        type="button"
        className="pagination-item"
        onClick={() => onPageChange(Math.max(0, page - 1))}
        disabled={page <= 0}
        aria-label="Page précédente"
      >
        ‹
      </button>
      {numeros.map((n, index) =>
        n === '…' ? (
          <span
            key={`ellipsis-${index}`}
            className="pagination-item"
            style={{ border: 'none', background: 'transparent' }}
            aria-hidden="true"
          >
            …
          </span>
        ) : (
          <button
            key={n}
            type="button"
            className={`pagination-item ${n === page ? 'active' : ''}`}
            onClick={() => onPageChange(n)}
            aria-label={`Page ${n + 1}`}
            aria-current={n === page ? 'page' : undefined}
          >
            {n + 1}
          </button>
        ),
      )}
      <button
        type="button"
        className="pagination-item"
        onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
        disabled={page >= totalPages - 1}
        aria-label="Page suivante"
      >
        ›
      </button>
    </div>
  )
}
