function [c, ceq] = xadd_constraints(args)

	<#list variables as variable>
	${variable.symbol} = args(${variable.index});
	</#list>

	<#if constraints??>
	<#list constraints as constraint>
	c(${constraint.index}) = ${constraint.function};
	</#list>
	</#if>

    ceq = [];
end