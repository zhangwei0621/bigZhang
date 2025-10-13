package com.study.openpdfdemo;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StructuredTextStripper extends PDFTextStripper {

    public static class Span {
        public final String text;
        public final float x;        // 左下角基线起点 X（页面坐标）
        public final float y;        // 左下角基线起点 Y（页面坐标）
        public final float fontSize;
        public final String fontName;

        public Span(String text, float x, float y, float fontSize, String fontName) {
            this.text = text; this.x = x; this.y = y; this.fontSize = fontSize; this.fontName = fontName;
        }
    }

    public static class Line {
        public final List<Span> spans = new ArrayList<>();
        public float y; // 行的代表性Y（用于排序/聚类）
    }

    public static class Block {
        public final List<Line> lines = new ArrayList<>();
        public float top, bottom;
    }

    private final List<Span> spans = new ArrayList<>();

    public StructuredTextStripper() throws Exception {
        setSortByPosition(true);           // 按页面视觉顺序抓取
        setWordSeparator(" ");             // 自定义词分隔（影响 writeString 的 text）
        setAddMoreFormatting(false);
        // 也可以 setStartPage / setEndPage 控制页码范围
    }

    @Override
    protected void writeString(String text, List<TextPosition> positions) {
        if (text == null || text.trim().isEmpty()) return;

        // 以该串的第一个字符的位置作为span起点（需要更细可逐char聚合）
        TextPosition first = positions.get(0);
        float x = first.getXDirAdj();
        float y = first.getYDirAdj();
        float fontSize = first.getFontSizeInPt();
        String fontName = "Unknown";
        try {
            if (first.getFont() != null) fontName = first.getFont().getName();
        } catch (Exception ignore) {}

        spans.add(new Span(text, x, y, fontSize, fontName));
    }

    /** 简单的行聚类：按Y坐标近似分组，再按X排序；可根据字号动态阈值优化 */
    public List<Block> toStructured(float lineMergeTolerance) {
        // 1) 先按Y降序（PDFBox坐标原点在左下，文本常从上到下 -> y大在上）
        spans.sort(Comparator.<Span>comparingDouble(s -> -s.y)
                             .thenComparingDouble(s -> s.x));

        List<Line> lines = new ArrayList<>();
        for (Span s : spans) {
            Line last = lines.isEmpty() ? null : lines.get(lines.size() - 1);
            if (last == null || Math.abs(last.y - s.y) > lineMergeTolerance) {
                Line nl = new Line();
                nl.y = s.y;
                nl.spans.add(s);
                lines.add(nl);
            } else {
                last.spans.add(s);
            }
        }
        // 行内再按X升序
        for (Line l : lines) {
            l.spans.sort(Comparator.comparingDouble(sp -> sp.x));
        }

        // 2) 简单块聚合（按行距阈值分段）
        List<Block> blocks = new ArrayList<>();
        Block cur = null;
        Float prevY = null;
        for (Line l : lines) {
            if (cur == null) {
                cur = new Block();
                cur.lines.add(l);
                cur.top = l.y; cur.bottom = l.y;
                blocks.add(cur);
            } else {
                if (prevY != null && (prevY - l.y) > (lineMergeTolerance * 2)) {
                    // 断开为新块
                    cur = new Block();
                    blocks.add(cur);
                }
                cur.lines.add(l);
                cur.top = Math.max(cur.top, l.y);
                cur.bottom = Math.min(cur.bottom, l.y);
            }
            prevY = l.y;
        }
        return blocks;
    }

    public static List<Block> extract(File pdf, int page, float lineMergeTolerance) throws Exception {
        try (PDDocument doc = PDDocument.load(pdf)) {
            StructuredTextStripper stripper = new StructuredTextStripper();
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            stripper.getText(doc); // 触发解析（会回调 writeString）
            return stripper.toStructured(lineMergeTolerance);
        }
    }

    // demo
    public static void main(String[] args) throws Exception {
        List<Block> blocks = extract(new File("input.pdf"), 1, 2.0f);
        for (Block b : blocks) {
            System.out.println("=== Block ===");
            for (Line l : b.lines) {
                StringBuilder sb = new StringBuilder();
                for (Span s : l.spans) {
                    sb.append(s.text);
                }
                System.out.printf("y=%.2f  %s%n", l.y, sb);
                for (Span s : l.spans) {
                    System.out.printf("   [%s] x=%.2f y=%.2f size=%.1f font=%s%n",
                            s.text, s.x, s.y, s.fontSize, s.fontName);
                }
            }
        }
    }
}