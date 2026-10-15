package cn.ilikui.libraryapi;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull(message = "总库存不能为空")
        @Min(value = 0, message = "总库存不能小于 0")
        Integer totalCopies
) {
}
