import Link from "next/link";
import Icon from "@/components/Icon";
export default function NotFound() {
  return (
    <main id="main-content" className="error-page">
      <p className="eyebrow">A LITTLE DETOUR · 404</p>
      <h1>This one slipped away.</h1>
      <p>
        The page or product you are looking for is not available. There are
        still good things to discover.
      </p>
      <Link className="button primary" href="/#collection">
        Back to the collection
        <Icon name="arrow" size={17} />
      </Link>
    </main>
  );
}
