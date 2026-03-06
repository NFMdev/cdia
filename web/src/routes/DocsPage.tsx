import { PagePlaceholder } from '../components/PagePlaceholder';

export function DocsPage() {
  return (
    <PagePlaceholder title="Docs" description="Project docs and runbooks.">
      <a
        className="text-sm font-medium text-accent underline"
        href="https://github.com/NFMdev/CDIA"
        target="_blank"
        rel="noreferrer"
      >
        Open project documentation
      </a>
    </PagePlaceholder>
  );
}
