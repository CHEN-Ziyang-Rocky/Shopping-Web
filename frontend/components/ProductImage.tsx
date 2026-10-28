"use client";
import Image from "next/image";
import { useState } from "react";
export default function ProductImage({
  src,
  alt,
  priority = false,
  sizes = "(max-width: 600px) 50vw, (max-width: 1000px) 33vw, 25vw",
}: {
  src?: string;
  alt: string;
  priority?: boolean;
  sizes?: string;
}) {
  const [failed, setFailed] = useState(false);
  return (
    <Image
      src={failed || !src ? "/catalog/placeholder.svg" : src}
      alt={alt}
      fill
      sizes={sizes}
      priority={priority}
      onError={() => setFailed(true)}
    />
  );
}
