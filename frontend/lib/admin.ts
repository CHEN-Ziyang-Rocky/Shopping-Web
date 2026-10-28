import { getCsrfToken } from "./api";
import { API_BASE, responseError } from "./shop";
export async function adminMutation(
  path: string,
  method: string,
  body?: FormData | URLSearchParams,
) {
  const token = await getCsrfToken();
  const response = await fetch(`${API_BASE}/admin/${path}`, {
    method,
    credentials: "include",
    headers: { "X-XSRF-TOKEN": token },
    body,
  });
  if (!response.ok)
    throw new Error(
      await responseError(
        response,
        "This change could not be saved. Please try again.",
      ),
    );
  return response;
}
