package cn.ilikui.libraryapi;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BookRequest(
        @NotBlank(message = "书名不能为空")
        @Size(max = 200, message = "书名不能超过 200 个字符")
        String title,
        @NotBlank(message = "作者不能为空")
        @Size(max = 120, message = "作者不能超过 120 个字符")
        String author,
        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.00", inclusive = true, message = "价格不能小于 0")
        BigDecimal price,
        @Size(max = 20, message = "ISBN 不能超过 20 个字符")
        @Pattern(regexp = "(?i)[0-9x-]{10,20}", message = "ISBN 仅允许数字、X 和连字符，长度为 10 到 20")
        String isbn
) {
}
