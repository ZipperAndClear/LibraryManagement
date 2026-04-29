import { get } from './http'

export const userList = () => {
  return get('/api/user/list')
}
