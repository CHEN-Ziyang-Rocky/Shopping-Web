export const everydayEdit = [
  {
    category: "Audio & Tech",
    name: "Everyday Headphones",
    price: 399,
    stock: 24,
    image: "headphones",
    description:
      "Find a little more focus in your everyday. An over-ear silhouette for your morning playlists, afternoon work sessions, and everything in between.",
  },
  {
    category: "Everyday Carry",
    name: "The Daily Backpack",
    price: 329,
    stock: 18,
    image: "backpack",
    description:
      "Take the day in your stride. A clean, understated backpack to keep your everyday essentials close, from your commute to a weekend wander.",
  },
  {
    category: "Desk & Living",
    name: "Studio Lounge Chair",
    price: 899,
    stock: 5,
    image: "chair",
    description:
      "A softer place to pause. A sculptural chair with a warm wood frame and a calm, textured finish, made for the corner you call your own.",
  },
  {
    category: "Audio & Tech",
    name: "Portable Speaker",
    price: 299,
    stock: 20,
    image: "speaker",
    description:
      "Bring your soundtrack along. A compact, considered speaker for easy listening at your desk or sharing a favourite playlist with good company.",
  },
  {
    category: "Everyday Carry",
    name: "Classic Everyday Watch",
    price: 459,
    stock: 12,
    image: "watch",
    description:
      "Make a little time for yourself. A classic watch silhouette with a warm leather look and a dial that brings a quiet detail to your everyday.",
  },
  {
    category: "Desk & Living",
    name: "The Reading Lamp",
    price: 249,
    stock: 16,
    image: "lamp",
    description:
      "A brighter little corner. A simple task lamp to bring a focused pool of light to your reading, creative work, and late-night ideas.",
  },
  {
    category: "Audio & Tech",
    name: "Everyday Camera",
    price: 1299,
    stock: 8,
    image: "camera",
    description:
      "Notice the little things. A camera-inspired addition to our everyday edit, celebrating the scenes, details, and memories worth keeping.",
  },
  {
    category: "Everyday Carry",
    name: "Weekend Sunglasses",
    price: 189,
    stock: 22,
    image: "sunglasses",
    description:
      "A fresh point of view. Rounded frames and an easy silhouette bring a little character to coffee runs, city walks, and unhurried weekends.",
  },
].map((p) => ({
  ...p,
  description:
    p.description +
    "\n\nPortfolio demonstration item. Illustrative photography; no brand affiliation or physical fulfilment. Stripe test payments only.",
}));
