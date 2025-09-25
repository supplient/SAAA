package com.example.strategicassetallocationassistant.ui.common.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * 金钱相关工具类
 * 提供统一的精度控制、舍入规则和格式化功能
 */
object MoneyUtils {
    
    // 精度配置
    const val MONEY_SCALE = 4                    // 金额字段精度 (4位小数)
    const val SHARE_SCALE = 6                    // 份额字段精度 (6位小数)
    const val PRICE_SCALE = 4                    // 价格字段精度 (4位小数)
    const val WEIGHT_SCALE = 8                   // 权重字段精度 (8位小数)
    
    // 舍入模式配置
    val DEFAULT_ROUNDING = RoundingMode.HALF_UP  // 默认舍入模式：四舍五入
    
    // 数学计算上下文
    val MONEY_CONTEXT = MathContext(20, DEFAULT_ROUNDING)    // 金额计算上下文
    val SHARE_CONTEXT = MathContext(20, DEFAULT_ROUNDING)    // 份额计算上下文
    
    // 常用的零值
    val ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE, DEFAULT_ROUNDING)
    val ZERO_SHARE = BigDecimal.ZERO.setScale(SHARE_SCALE, DEFAULT_ROUNDING)
    val ZERO_PRICE = BigDecimal.ZERO.setScale(PRICE_SCALE, DEFAULT_ROUNDING)
    
    // 常用的一值
    val ONE_MONEY = BigDecimal.ONE.setScale(MONEY_SCALE, DEFAULT_ROUNDING)
    val ONE_SHARE = BigDecimal.ONE.setScale(SHARE_SCALE, DEFAULT_ROUNDING)
    val ONE_PRICE = BigDecimal.ONE.setScale(PRICE_SCALE, DEFAULT_ROUNDING)
    
    /**
     * 创建金额BigDecimal (4位小数精度)
     */
    fun createMoney(value: String): BigDecimal = 
        BigDecimal(value).setScale(MONEY_SCALE, DEFAULT_ROUNDING)
    
    fun createMoney(value: Double): BigDecimal = 
        BigDecimal.valueOf(value).setScale(MONEY_SCALE, DEFAULT_ROUNDING)
    
    fun createMoney(value: Int): BigDecimal = 
        BigDecimal.valueOf(value.toLong()).setScale(MONEY_SCALE, DEFAULT_ROUNDING)
    
    /**
     * 创建份额BigDecimal (6位小数精度)
     */
    fun createShare(value: String): BigDecimal = 
        BigDecimal(value).setScale(SHARE_SCALE, DEFAULT_ROUNDING)
    
    fun createShare(value: Double): BigDecimal = 
        BigDecimal.valueOf(value).setScale(SHARE_SCALE, DEFAULT_ROUNDING)
    
    /**
     * 创建价格BigDecimal (4位小数精度)
     */
    fun createPrice(value: String): BigDecimal = 
        BigDecimal(value).setScale(PRICE_SCALE, DEFAULT_ROUNDING)
    
    fun createPrice(value: Double): BigDecimal = 
        BigDecimal.valueOf(value).setScale(PRICE_SCALE, DEFAULT_ROUNDING)
    
    /**
     * 安全的除法运算，避免无限小数
     */
    fun safeDivide(
        dividend: BigDecimal, 
        divisor: BigDecimal, 
        scale: Int = MONEY_SCALE,
        roundingMode: RoundingMode = DEFAULT_ROUNDING
    ): BigDecimal {
        return if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO.setScale(scale, roundingMode)
        } else {
            dividend.divide(divisor, scale, roundingMode)
        }
    }
    
    /**
     * 安全的乘法运算
     */
    fun safeMultiply(
        multiplicand: BigDecimal,
        multiplier: BigDecimal,
        scale: Int = MONEY_SCALE,
        roundingMode: RoundingMode = DEFAULT_ROUNDING
    ): BigDecimal {
        return multiplicand.multiply(multiplier).setScale(scale, roundingMode)
    }
    
    /**
     * 格式化为金额字符串 (3位小数显示)
     */
    fun formatMoney(amount: BigDecimal): String {
        return "¥${amount.setScale(3, DEFAULT_ROUNDING)}"
    }
    
    /**
     * 格式化为金额字符串，指定小数位数
     */
    fun formatMoney(amount: BigDecimal, scale: Int): String {
        return "¥${amount.setScale(scale, DEFAULT_ROUNDING)}"
    }
    
    /**
     * 格式化为百分比字符串
     */
    fun formatPercentage(value: BigDecimal, scale: Int = 2): String {
        val percentage = value.multiply(BigDecimal("100")).setScale(scale, DEFAULT_ROUNDING)
        return "${percentage}%"
    }
    
    /**
     * 格式化份额 (2位小数显示)
     */
    fun formatShare(shares: BigDecimal): String {
        return "×${shares.setScale(2, DEFAULT_ROUNDING)}"
    }
    
    /**
     * 格式化单价 (4位小数显示)
     */
    fun formatPrice(price: BigDecimal): String {
        return "¥${price.setScale(4, DEFAULT_ROUNDING)}"
    }
    
    /**
     * 格式化为金额字符串，不带货币符号 (4位小数显示)
     */
    fun formatMoneyPlain(amount: BigDecimal): String {
        return amount.setScale(4, DEFAULT_ROUNDING).toString()
    }
    
    /**
     * 判断两个BigDecimal是否在指定精度下相等
     */
    fun isEqual(a: BigDecimal?, b: BigDecimal?, scale: Int = MONEY_SCALE): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        
        val aScaled = a.setScale(scale, DEFAULT_ROUNDING)
        val bScaled = b.setScale(scale, DEFAULT_ROUNDING)
        return aScaled.compareTo(bScaled) == 0
    }
    
    /**
     * 判断BigDecimal是否为零
     */
    fun isZero(value: BigDecimal?): Boolean {
        return value?.compareTo(BigDecimal.ZERO) == 0
    }
    
    /**
     * 判断BigDecimal是否为正数
     */
    fun isPositive(value: BigDecimal?): Boolean {
        return value?.compareTo(BigDecimal.ZERO) == 1
    }
    
    /**
     * 判断BigDecimal是否为负数
     */
    fun isNegative(value: BigDecimal?): Boolean {
        return value?.compareTo(BigDecimal.ZERO) == -1
    }
}

/**
 * BigDecimal扩展函数
 * 提供便捷的操作方法
 */

/**
 * 转换为金额格式 (4位小数精度)
 */
fun BigDecimal.toMoney(): BigDecimal = 
    this.setScale(MoneyUtils.MONEY_SCALE, MoneyUtils.DEFAULT_ROUNDING)

/**
 * 转换为份额格式 (6位小数精度)
 */
fun BigDecimal.toShare(): BigDecimal = 
    this.setScale(MoneyUtils.SHARE_SCALE, MoneyUtils.DEFAULT_ROUNDING)

/**
 * 转换为价格格式 (4位小数精度)
 */
fun BigDecimal.toPrice(): BigDecimal = 
    this.setScale(MoneyUtils.PRICE_SCALE, MoneyUtils.DEFAULT_ROUNDING)

/**
 * 格式化为金额字符串
 */
fun BigDecimal.toMoneyString(): String = MoneyUtils.formatMoney(this)

/**
 * 格式化为份额字符串
 */
fun BigDecimal.toShareString(): String = MoneyUtils.formatShare(this)

/**
 * 格式化为价格字符串
 */
fun BigDecimal.toPriceString(): String = MoneyUtils.formatPrice(this)

/**
 * 格式化为百分比字符串
 */
fun BigDecimal.toPercentageString(scale: Int = 2): String = MoneyUtils.formatPercentage(this, scale)

/**
 * 安全除法，避免除零错误
 */
fun BigDecimal.safeDivide(
    divisor: BigDecimal, 
    scale: Int = MoneyUtils.MONEY_SCALE
): BigDecimal = MoneyUtils.safeDivide(this, divisor, scale)

/**
 * 安全乘法
 */
fun BigDecimal.safeMultiply(
    multiplier: BigDecimal,
    scale: Int = MoneyUtils.MONEY_SCALE
): BigDecimal = MoneyUtils.safeMultiply(this, multiplier, scale)

/**
 * 判断是否为零
 */
fun BigDecimal.isZero(): Boolean = this.compareTo(BigDecimal.ZERO) == 0

/**
 * 判断是否为正数
 */
fun BigDecimal.isPositive(): Boolean = this.compareTo(BigDecimal.ZERO) > 0

/**
 * 判断是否为负数
 */
fun BigDecimal.isNegative(): Boolean = this.compareTo(BigDecimal.ZERO) < 0

/**
 * 绝对值
 */
fun BigDecimal.absoluteValue(): BigDecimal = this.abs()

/**
 * 取反 (已有成员函数，此扩展函数移除)
 */
// fun BigDecimal.negate(): BigDecimal = this.negate() // 移除：与成员函数重复

/**
 * String扩展函数，转换为BigDecimal
 */

/**
 * 字符串转换为金额BigDecimal
 */
fun String.toBigDecimalMoney(): BigDecimal? = try {
    MoneyUtils.createMoney(this)
} catch (e: NumberFormatException) {
    null
}

/**
 * 字符串转换为份额BigDecimal
 */
fun String.toBigDecimalShare(): BigDecimal? = try {
    MoneyUtils.createShare(this)
} catch (e: NumberFormatException) {
    null
}

/**
 * 字符串转换为价格BigDecimal
 */
fun String.toBigDecimalPrice(): BigDecimal? = try {
    MoneyUtils.createPrice(this)
} catch (e: NumberFormatException) {
    null
}

/**
 * Double扩展函数，转换为BigDecimal
 */

/**
 * Double转换为金额BigDecimal
 */
fun Double.toBigDecimalMoney(): BigDecimal = MoneyUtils.createMoney(this)

/**
 * Double转换为份额BigDecimal
 */
fun Double.toBigDecimalShare(): BigDecimal = MoneyUtils.createShare(this)

/**
 * Double转换为价格BigDecimal
 */
fun Double.toBigDecimalPrice(): BigDecimal = MoneyUtils.createPrice(this)
