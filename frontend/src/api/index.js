import { createStore } from 'vuex'
import user from '../store/modules/user'
import announcement from '../store/modules/announcement'
import product from '../store/modules/product'
import inventory from '../store/modules/inventory'
import purchase from '../store/modules/purchase'
import sales from '../store/modules/sales'

export default createStore({
    modules: {
        user,
        announcement,
        product,
        inventory,
        purchase,
        sales
    }
})