import type { ReactNode } from "react";
import {
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";

export function ExperienceShell({
  kicker,
  title,
  subtitle,
  children,
}: {
  kicker: string;
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <SafeAreaView style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.hero}>
          <Text style={styles.logo}>
            stick<Text style={styles.logoBadge}> me</Text>
          </Text>
          <Text style={styles.kicker}>{kicker}</Text>
          <Text style={styles.title}>{title}</Text>
          <Text style={styles.subtitle}>{subtitle}</Text>
        </View>
        {children}
      </ScrollView>
    </SafeAreaView>
  );
}

export function ExperienceCard({
  title,
  body,
  tone = "dark",
  onPress,
}: {
  title: string;
  body: string;
  tone?: "dark" | "red" | "green";
  onPress?: () => void;
}) {
  const content = (
    <View style={[styles.card, tone === "red" && styles.redCard, tone === "green" && styles.greenCard]}>
      <Text style={styles.cardTitle}>{title}</Text>
      <Text style={styles.cardBody}>{body}</Text>
    </View>
  );

  if (!onPress) return content;

  return <Pressable onPress={onPress}>{content}</Pressable>;
}

export function ExperienceGateRow({
  label,
  value,
  passed,
}: {
  label: string;
  value: string;
  passed: boolean;
}) {
  return (
    <View style={styles.row}>
      <View>
        <Text style={styles.rowLabel}>{label}</Text>
        <Text style={styles.rowValue}>{value}</Text>
      </View>
      <Text style={[styles.badge, passed ? styles.badgePass : styles.badgeBlock]}>
        {passed ? "PASS" : "BLOCK"}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#050505" },
  content: { padding: 20, gap: 16 },
  hero: { gap: 8, paddingVertical: 12 },
  logo: { color: "#ffffff", fontSize: 28, fontWeight: "900", letterSpacing: -1 },
  logoBadge: { color: "#B9FF3D" },
  kicker: { color: "#B9FF3D", fontSize: 12, fontWeight: "800", textTransform: "uppercase" },
  title: { color: "#ffffff", fontSize: 30, fontWeight: "900", lineHeight: 34 },
  subtitle: { color: "#c9c9c9", fontSize: 15, lineHeight: 21 },
  card: { backgroundColor: "#151515", borderRadius: 22, padding: 18, gap: 8, borderWidth: 1, borderColor: "#262626" },
  redCard: { borderColor: "#FF4545" },
  greenCard: { borderColor: "#B9FF3D" },
  cardTitle: { color: "#ffffff", fontSize: 17, fontWeight: "900" },
  cardBody: { color: "#d8d8d8", fontSize: 14, lineHeight: 20 },
  row: { backgroundColor: "#101010", borderRadius: 16, padding: 14, flexDirection: "row", justifyContent: "space-between", gap: 12, borderWidth: 1, borderColor: "#232323" },
  rowLabel: { color: "#ffffff", fontWeight: "800", fontSize: 14 },
  rowValue: { color: "#a8a8a8", marginTop: 3, maxWidth: 230 },
  badge: { overflow: "hidden", borderRadius: 999, paddingHorizontal: 10, paddingVertical: 5, fontSize: 11, fontWeight: "900", alignSelf: "center" },
  badgePass: { backgroundColor: "#B9FF3D", color: "#050505" },
  badgeBlock: { backgroundColor: "#FF4545", color: "#ffffff" },
});
