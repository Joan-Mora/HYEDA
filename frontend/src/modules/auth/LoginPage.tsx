import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { Logo } from '../../brand/Logo'
import { Button, Input } from '../../components/ui'
import { useToast } from '../../components/Toast'
import { getErrorMessage } from '../../lib/format'
import { useAuth } from './AuthContext'

const schema = z.object({
  username: z.string().min(1, 'El usuario es obligatorio'),
  password: z.string().min(1, 'La contrasena es obligatoria'),
})

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const form = useForm({ resolver: zodResolver(schema), defaultValues: { username: 'admin', password: '' } })

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <section className="hidden items-center bg-navy p-12 text-white lg:flex">
        <div>
          <Logo size={72} />
          <h1 className="mt-6 font-display text-5xl">HYEDA</h1>
          <p className="mt-3 max-w-md text-white/80">
            Centro de control del patio de Herramientas y Equipos del Ariari. Granada, Meta.
          </p>
        </div>
      </section>
      <section className="flex items-center justify-center p-6">
        <form
          className="w-full max-w-sm space-y-4 rounded-[10px] bg-white p-6 shadow"
          onSubmit={form.handleSubmit(async (values) => {
            try {
              await login(values.username, values.password)
              navigate('/')
            } catch (error) {
              toast.push(getErrorMessage(error), 'error')
            }
          })}
        >
          <h2 className="font-display text-2xl text-navy">Entrar al patio</h2>
          <Input label="Usuario" error={form.formState.errors.username?.message} {...form.register('username')} />
          <Input label="Contrasena" type="password" error={form.formState.errors.password?.message} {...form.register('password')} />
          <Button className="w-full" loading={form.formState.isSubmitting}>Entrar</Button>
        </form>
      </section>
    </div>
  )
}
