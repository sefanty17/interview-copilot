package cn.edu.zjut.interview.dto.worldbook;

import cn.edu.zjut.interview.domain.WorldbookBook;

import java.util.List;

/** 世界书详情：书级信息 + 按 YAML 顺序排好的全部条目。 */
public record WorldbookDetail(String code, String jobCode, String name, String description,
                              String sourcePath, List<WorldbookEntryView> entries) {

    public static WorldbookDetail of(WorldbookBook book, List<WorldbookEntryView> entries) {
        return new WorldbookDetail(
                book.getCode(),
                book.getJobCode(),
                book.getName(),
                book.getDescription(),
                book.getSourcePath(),
                entries);
    }
}
