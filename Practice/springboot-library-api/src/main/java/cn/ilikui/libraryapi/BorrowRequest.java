package cn.ilikui.libraryapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BorrowRequest(
        @NotBlank(message = "借阅人姓名不能为空")
        @Size(max = 120, message = "借阅人姓名不能超过 120 个字符")
        String borrowerName
) {
}
