import AuthForm from "@/components/AuthForm";

export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ next?: string | string[] }>;
}) {
  const { next } = await searchParams;
  return <AuthForm mode="login" returnToBag={next === "bag"} />;
}
