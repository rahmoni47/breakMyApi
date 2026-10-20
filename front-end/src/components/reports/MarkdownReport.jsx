import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

export default function MarkdownReport({ markdown }) {
  return (
    <article className="prose prose-slate max-w-none rounded-xl bg-white p-6 shadow-card prose-headings:text-text-main prose-p:text-text-secondary prose-strong:text-text-main prose-table:w-full prose-th:bg-soft-blue prose-th:text-xs prose-th:uppercase prose-td:text-sm">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{markdown}</ReactMarkdown>
    </article>
  );
}
