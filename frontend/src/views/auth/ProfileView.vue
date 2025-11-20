<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useAuthStore} from '@/stores/auth'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Dropdown from 'primevue/dropdown'
import MultiSelect from 'primevue/multiselect'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Divider from 'primevue/divider'

const authStore = useAuthStore()

const displayName = ref('')
const nativeLanguage = ref('')
const learningLanguages = ref<string[]>([])
const isEditing = ref(false)
const saveSuccess = ref(false)

const languageOptions = [
  {label: 'English', value: 'en'}, {label: 'Spanish', value: 'es'}, {label: 'French', value: 'fr'},
  {label: 'German', value: 'de'}, {label: 'Italian', value: 'it'}, {label: 'Portuguese', value: 'pt'},
  {label: 'Russian', value: 'ru'}, {label: 'Chinese', value: 'zh'}, {label: 'Japanese', value: 'ja'},
  {label: 'Korean', value: 'ko'}, {label: 'Arabic', value: 'ar'}, {label: 'Hindi', value: 'hi'},
]

const availableLearningLanguages = computed(() => languageOptions.filter(lang => lang.value !== nativeLanguage.value))
const getLanguageLabel = (code: string) => languageOptions.find(l => l.value === code)?.label || code

onMounted(() => {
  if (authStore.user) {
    displayName.value = authStore.user.displayName || ''
    nativeLanguage.value = authStore.user.nativeLanguage
    learningLanguages.value = [...authStore.user.learningLanguages]
  }
})

const handleCancel = () => {
  if (authStore.user) {
    displayName.value = authStore.user.displayName || ''
    nativeLanguage.value = authStore.user.nativeLanguage
    learningLanguages.value = [...authStore.user.learningLanguages]
  }
  isEditing.value = false
}

const handleSave = async () => {
  const success = await authStore.updateProfile({
    displayName: displayName.value || null,
    nativeLanguage: nativeLanguage.value,
    learningLanguages: learningLanguages.value,
  })
  if (success) {
    isEditing.value = false
    saveSuccess.value = true
    setTimeout(() => saveSuccess.value = false, 3000)
  }
}
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1 class="flex items-center gap-md">
        <i class="pi pi-user text-3xl icon-primary"></i>
        My Profile
      </h1>
    </div>

    <Message v-if="saveSuccess" severity="success">Profile updated successfully!</Message>
    <Message v-if="authStore.error" severity="error">{{ authStore.error }}</Message>

    <Card>
      <template #content>
        <div class="content-area">
          <div>
            <h3 class="flex items-center gap-md text-xl font-semibold text-primary mb-lg">
              <i class="pi pi-envelope"></i> Email Address
            </h3>
            <div class="pl-4xl">
              <label class="font-medium mb-sm block">Email</label>
              <InputText :model-value="authStore.user?.email" disabled class="w-full"/>
              <small class="text-secondary mt-sm block">Email cannot be changed.</small>
            </div>
          </div>

          <Divider/>

          <div>
            <h3 class="flex items-center gap-md text-xl font-semibold text-primary mb-lg">
              <i class="pi pi-id-card"></i> Personal Information
            </h3>
            <div class="pl-4xl">
              <label for="displayName" class="font-medium mb-sm block">Display Name</label>
              <InputText id="displayName" v-model="displayName" :disabled="!isEditing"
                         placeholder="Enter your display name" class="w-full"/>
            </div>
          </div>

          <Divider/>

          <div>
            <h3 class="flex items-center gap-md text-xl font-semibold text-primary mb-lg">
              <i class="pi pi-language"></i> Language Preferences
            </h3>
            <div class="pl-4xl content-area">
              <div>
                <label for="nativeLanguage" class="font-medium mb-sm block">Native Language</label>
                <Dropdown id="nativeLanguage" v-model="nativeLanguage" :options="languageOptions" option-label="label"
                          option-value="value" :disabled="!isEditing" placeholder="Select your native language"
                          class="w-full"/>
              </div>
              <div>
                <label for="learningLanguages" class="font-medium mb-sm block">Learning Languages</label>
                <MultiSelect id="learningLanguages" v-model="learningLanguages" :options="availableLearningLanguages"
                             option-label="label" option-value="value" :disabled="!isEditing"
                             placeholder="Select languages" class="w-full" display="chip"/>
              </div>
            </div>
          </div>

          <div class="mt-xl pt-xl flex gap-md justify-end border-t border-surface">
            <Button v-if="!isEditing" label="Edit Profile" icon="pi pi-pencil" @click="isEditing = true"/>
            <template v-else>
              <Button label="Cancel" icon="pi pi-times" severity="secondary" @click="handleCancel"/>
              <Button label="Save Changes" icon="pi pi-check" :loading="authStore.loading" @click="handleSave"/>
            </template>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>
