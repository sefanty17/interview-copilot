package cn.edu.zjut.interview.dto.worldbook;

import cn.edu.zjut.interview.domain.WorldbookBook;

/** 世界书列表项：只给目录信息，不带条目正文。 */
public record WorldbookSummary(String code, String name, String description, int entryCount, boolean enabled) {

    public static WorldbookSummary of(WorldbookBook book, long entryCount) {
        return new WorldbookSummary(
                book.getCode(),
                book.getName(),
                book.getDescription(),
                (int) entryCount,
                Boolean.TRUE.equals(book.getEnabled()));
    }
}
