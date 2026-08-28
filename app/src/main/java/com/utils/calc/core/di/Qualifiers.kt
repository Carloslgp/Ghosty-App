package com.utils.calc.core.di

import javax.inject.Qualifier

/**
 * Repositorio que respeita o cofre aberto no momento (real ou coacao). E' o que
 * as telas do cofre injetam. O binding sem qualificador entrega sempre os dados
 * verdadeiros — e' o que o fluxo de panico usa, porque um alerta disparado de
 * dentro do cofre falso ainda precisa avisar os contatos reais.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionAware

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
