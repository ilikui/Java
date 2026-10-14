# Lambda、Stream 与 Optional

## Lambda 与函数式接口

函数式接口只有一个抽象方法，可以用 Lambda 表达式提供实现。常见类型包括 Predicate、Function、Consumer 与 Supplier。

```java
import java.util.List;
import java.util.stream.Collectors;

public class StreamExample {
    public static List<String> activeNames(List<String> names) {
        return names.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
```

示例先过滤无效值，再转换、去重和排序。数据规模、顺序要求及副作用都会影响设计。

## Stream 的关键特点

- 中间操作（如 filter、map）通常是惰性的。
- 终止操作（如 collect、forEach、count）触发流水线执行。
- Stream 通常不应重复消费。
- 避免在流水线中修改共享可变状态。
- 并行流并非自动更快；任务粒度、拆分成本、共享资源和顺序要求都很重要。

## Optional

Optional 适合表达某个返回值可能不存在的情况。它不是所有字段、参数和集合的通用替代品。避免无意义地调用 `get()`，优先使用 `map`、`flatMap`、`orElseGet` 等表达意图。

## 练习

1. 从订单列表筛选已支付订单，计算总金额。
2. 将字符串列表转换为去空白、去重、排序后的结果。
3. 比较循环写法与 Stream 写法的可读性和性能，不要预设某种写法总是更快。

## 面试自测

- Stream 的惰性体现在哪里？
- map 与 flatMap 的区别是什么？
- orElse 与 orElseGet 的求值差异是什么？
- 什么情况下不应该使用并行流？
