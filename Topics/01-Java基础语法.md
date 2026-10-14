# Java 基础语法

## 本专题目标

能够创建、编译、运行简单 Java 程序，理解类型、流程控制、数组与方法，并能用调试器定位基本错误。

## 1. 程序从哪里开始

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Java!");
    }
}
```

保存为 `Main.java`，执行：

```bash
javac Main.java
java Main
```

类名与文件名在公开顶层类场景下需要匹配。编译阶段检查语法与类型，运行阶段执行字节码。

## 2. 基本类型与引用类型

| 类别 | 类型 | 说明 |
|---|---|---|
| 整数 | byte、short、int、long | 注意取值范围与溢出 |
| 浮点 | float、double | 二进制浮点不适合直接表示所有十进制小数 |
| 字符 | char | UTF-16 代码单元，不等同于所有用户感知字符 |
| 布尔 | boolean | 表达 true / false |
| 引用类型 | 类、数组、接口等 | 变量保存引用值，可为 null |

金额计算通常使用 `BigDecimal`，并明确舍入规则；不要依赖 `double` 的精确十进制表示。

## 3. 流程控制

```java
int score = 86;
if (score >= 90) {
    System.out.println("优秀");
} else if (score >= 60) {
    System.out.println("通过");
} else {
    System.out.println("需要复习");
}

for (int i = 0; i < 5; i++) {
    System.out.println(i);
}
```

- `if / else`：按条件选择分支。
- `switch`：按离散值分派；具体可用语法取决于 JDK 版本。
- `for`：已知或可表达迭代条件的循环。
- `while`：先判断再执行；`do-while` 至少执行一次。
- `break` 结束循环或 switch；`continue` 进入下一次循环。

## 4. 数组与方法

```java
static int sum(int[] values) {
    if (values == null) {
        throw new IllegalArgumentException("values must not be null");
    }
    int total = 0;
    for (int value : values) {
        total += value;
    }
    return total;
}
```

数组长度固定。访问不存在的下标会抛出 `ArrayIndexOutOfBoundsException`。设计方法时要明确参数约束、返回值和异常行为。

## 5. 常见误区

- 用 `==` 比较字符串内容；通常应使用 `equals`。
- 忘记整数除法会截断小数部分。
- 假定数组和对象引用一定非 null。
- 忽略整数溢出和输入边界。
- 把编译错误与运行时异常混为一谈。

## 练习

1. 编写一个方法，返回整数数组中的最大值；空数组如何处理由你定义并写入文档。
2. 统计 1 到 100 之间能被 3 或 5 整除的数字之和。
3. 写一个安全的除法方法，明确分母为零时的行为。
4. 使用调试器观察循环变量和方法调用栈。

## 面试自测

- JDK、JRE、JVM 分别是什么？
- 基本类型与引用类型有什么区别？
- `==` 与 `equals` 有何区别？
- Java 是值传递还是引用传递？请用对象参数示例说明。
