package arrow.optics

import arrow.common.utils.simpleName
import me.eugeniomarletti.kotlin.metadata.plusIfNotBlank

fun generateOptionals(ele: AnnotatedElement, target: OptionalTarget) = Snippet(
  `package` = ele.packageName,
  name = ele.classData.simpleName,
  imports = setOf("import arrow.core.left", "import arrow.core.right", "import arrow.core.toOption"),
  content = processElement(ele, target.foci)
)

private fun processElement(ele: AnnotatedElement, foci: List<Focus>): String = foci.joinToString(separator = "\n") { focus ->
  fun getOrModifyF(toNullable: String = "") = "{ ${ele.sourceName}: ${ele.sourceClassName} -> ${ele.sourceName}.${focus.paramName.plusIfNotBlank(prefix = "`", postfix = "`")}$toNullable?.right() ?: ${ele.sourceName}.left() }"
  fun setF(fromNullable: String = "") = "${ele.sourceName}.copy(${focus.paramName.plusIfNotBlank(prefix = "`", postfix = "`")} = value$fromNullable)"

  val (targetClassName, getOrModify, set) = when (focus) {
    is NullableFocus -> Triple(focus.nonNullClassName, getOrModifyF(), setF())
    is OptionFocus -> Triple(focus.nestedClassName, getOrModifyF(".orNull()"), setF(".toOption()"))
    is NonNullFocus -> return@joinToString ""
  }

  """
      |inline val ${ele.sourceClassName}.Companion.${focus.paramName}: $Optional<${ele.sourceClassName}, $targetClassName> inline get()= $Optional(
      |  getOrModify = $getOrModify,
      |  set = { value: $targetClassName ->
      |    { ${ele.sourceName}: ${ele.sourceClassName} ->
      |      $set
      |    }
      |  }
      |)
      |""".trimMargin()
}