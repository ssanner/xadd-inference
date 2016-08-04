function val = xadd_objective_func(args)

    <#list variables as variable>
	${variable.symbol} = args(${variable.index})
    </#list>

    <#if constants??>
    <#list constants as constant>
	${constant.symbol} = ${constant.value}
    </#list>
    </#if>

    val = ${objective};
end