package com.example.form.codegen

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import java.io.OutputStream

class FormMapperProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GenerateFormMapper::class.qualifiedName!!)
        val ret = symbols.filter { !it.validate() }.toList()

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(FormMapperVisitor(codeGenerator, logger), Unit) }

        return ret
    }
}

class FormMapperVisitor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : KSVisitorVoid() {

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val packageName = classDeclaration.containingFile!!.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val qualifiedName = classDeclaration.qualifiedName!!.asString()
        val fileName = "${className}FormMapper"

        val outputStream: OutputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(true, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = fileName
        )

        val properties = classDeclaration.getAllProperties().map { it.simpleName.asString() }.toList()

        outputStream.write(
            buildString {
                append("package $packageName\n\n")
                append("import com.example.form.core.Form\n")
                append("import com.example.form.core.valueFor\n")
                append("import $qualifiedName\n\n")

                append("fun Form<$className>.getDataAs$className(): $className {\n")
                append("    return $className(\n")
                properties.forEach { propName ->
                    append("        $propName = this.valueFor($className::$propName),\n")
                }
                append("    )\n")
                append("}")
            }.toByteArray()
        )

        outputStream.close()
    }
}
