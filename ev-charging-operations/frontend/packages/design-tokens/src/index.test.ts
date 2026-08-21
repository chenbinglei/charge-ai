import { describe, expect, it } from "vitest";
import { isThemeName, layoutTokens, themeNames } from "./index";

describe("W1 设计 Token", () => {
  it("固定三种主题且保持 24 栅格和 8px 间距", () => {
    expect(themeNames).toHaveLength(3);
    expect(isThemeName("energy-green")).toBe(true);
    expect(isThemeName("unapproved-theme")).toBe(false);
    expect(layoutTokens.gridColumns).toBe(24);
    expect(layoutTokens.spacingUnit).toBe("8px");
  });
});
