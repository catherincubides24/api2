export default function SectionTitle({ eyebrow, title, description, align = "left" }) {
  const alignClass = align === "center" ? "text-center" : "text-left";

  return (
    <div className={["space-y-2", alignClass].join(" ")}>
      {eyebrow && (
        <p className="text-xs font-semibold uppercase tracking-[0.25em] text-mint">
          {eyebrow}
        </p>
      )}
      <h2 className="font-display text-3xl leading-tight text-ink sm:text-4xl">{title}</h2>
      {description && <p className="max-w-2xl text-ink/70">{description}</p>}
    </div>
  );
}
