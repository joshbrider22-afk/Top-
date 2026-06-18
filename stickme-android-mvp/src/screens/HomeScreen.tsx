import { ExperienceCard, ExperienceShell } from "../components/ExperienceShell";

export function HomeScreen() {
  return (
    <ExperienceShell
      kicker="Private family sticker keyboard"
      title="Turn family memories into stickers."
      subtitle="A clean consumer home: create, save, open keyboard, share. No founder dashboard sprawl."
    >
      <ExperienceCard
        title="1. Create sticker"
        body="Choose a family photo and turn the moment into a local sticker."
        tone="green"
      />
      <ExperienceCard
        title="2. Save to pack"
        body="Keep the sticker private on device in the family sticker pack."
      />
      <ExperienceCard
        title="3. Share from keyboard"
        body="Open StickMe Keyboard and send the sticker into the message flow."
      />
    </ExperienceShell>
  );
}
